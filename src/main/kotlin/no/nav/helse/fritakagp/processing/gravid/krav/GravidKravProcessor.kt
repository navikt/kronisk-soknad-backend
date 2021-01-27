package no.nav.helse.fritakagp.processing.gravid.krav

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.*
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlIdent
import no.nav.helse.fritakagp.GravidKravMetrics
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKafkaProcessor
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class GravidKravProcessor(
    private val gravidKravRepo: GravidKravRepository,
    private val dokarkivKlient: DokarkivKlient,
    private val oppgaveKlient: OppgaveKlient,
    private val pdlClient: PdlClient,
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
    private val pdfGenerator: GravidKravPDFGenerator,
    private val om: ObjectMapper,
    private val bucketStorage: BucketStorage
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "gravid-krav-formidling"
        val dokumentasjonBrevkode = "krav_om_fritak_fra_agp_dokumentasjon"
    }

    val digitalKravBehandingsType = "ae0227"
    val fritakAGPBehandingsTema = "ab0338"

    val log = LoggerFactory.getLogger(GravidKravProcessor::class.java)

    /**
     * Prosesserer et gravidkrav; journalfører kravet og oppretter en oppgave for saksbehandler.
     * Jobbdataene forventes å være en UUID for et krav som skal prosesseres.
     */
    override fun prosesser(jobbDataString: String) {
        val jobbData = om.readValue<JobbData>(jobbDataString)
        val krav = gravidKravRepo.getById(jobbData.id)
        requireNotNull(krav, { "Jobben indikerte et krav med id $jobbData men den kunne ikke finnes" })

        try {
            if (krav.journalpostId == null) {
                krav.journalpostId = journalfør(krav)
                GravidKravMetrics.tellJournalfoert()
            }

            bucketStorage.deleteDoc(krav.id)

            if (krav.oppgaveId == null) {
                krav.oppgaveId = opprettOppgave(krav)
                GravidKravMetrics.tellOppgaveOpprettet()
            }
            bakgrunnsjobbRepo.save(
                Bakgrunnsjobb(
                    maksAntallForsoek = 10,
                    data = om.writeValueAsString(GravidKravKafkaProcessor.JobbData(krav.id)),
                    type = GravidKravKafkaProcessor.JOB_TYPE
                )
            )
        } finally {
            updateAndLogOnFailure(krav)
        }
    }

    private fun updateAndLogOnFailure(krav: GravidKrav) {
        try {
            gravidKravRepo.update(krav)
        } catch (e: Exception) {
            throw RuntimeException("Feilet i å lagre ${krav.id} etter at en ekstern operasjon har blitt utført. JournalpostID: ${krav.journalpostId} OppgaveID: ${krav.oppgaveId}", e)
        }
    }

    fun journalfør(krav: GravidKrav): String {
        val journalfoeringsTittel = "Krav om fritak fra arbeidsgiverperioden ifbm graviditet"
        val pdlResponse = pdlClient.personNavn(krav.sendtAv)?.navn?.firstOrNull()
        val innsenderNavn = if (pdlResponse != null) "${pdlResponse.fornavn} ${pdlResponse.etternavn}" else "Ukjent"

        val response = dokarkivKlient.journalførDokument(
            JournalpostRequest(
                tittel = journalfoeringsTittel,
                journalposttype = Journalposttype.INNGAAENDE,
                kanal = "NAV_NO",
                bruker = Bruker(krav.identitetsnummer, IdType.FNR),
                eksternReferanseId = krav.id.toString(),
                avsenderMottaker = AvsenderMottaker(
                    id = krav.sendtAv,
                    idType = IdType.FNR,
                    navn = innsenderNavn
                ),
                dokumenter = createDocuments(krav, journalfoeringsTittel),
                datoMottatt = krav.opprettet.toLocalDate()
            ), true, UUID.randomUUID().toString()

        )

        log.debug("Journalført ${krav.id} med ref ${response.journalpostId}")
        return response.journalpostId
    }

    private fun createDocuments(
        krav: GravidKrav,
        journalfoeringsTittel: String
    ): List<Dokument> {
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagPDF(krav))


        val dokumentListe = mutableListOf(
            Dokument(
                dokumentVarianter = listOf(
                    DokumentVariant(
                        fysiskDokument = base64EnkodetPdf
                    )
                ),
                brevkode = "krav_om_fritak_fra_agp_gravid",
                tittel = journalfoeringsTittel,
            )
        )

        bucketStorage.getDocAsString(krav.id)?.let {
            dokumentListe.add(
                Dokument(
                    dokumentVarianter = listOf(
                        DokumentVariant(
                            fysiskDokument = it.base64Data,
                            filtype = if (it.extension == "jpg") "JPEG" else it.extension.toUpperCase()
                        )
                    ),
                    brevkode = dokumentasjonBrevkode,
                    tittel = "Helsedokumentasjon",
                )
            )
        }

        return dokumentListe
    }

    fun opprettOppgave(krav: GravidKrav): String {
        val aktoerId = pdlClient.fullPerson(krav.identitetsnummer)?.hentIdenter?.trekkUtIdent(PdlIdent.PdlIdentGruppe.AKTORID)
        requireNotNull(aktoerId, { "Fant ikke AktørID for fnr i ${krav.id}" })

        val request = OpprettOppgaveRequest(
            aktoerId = aktoerId,
            journalpostId = krav.journalpostId,
            beskrivelse = """
                Krav om refusjon av arbeidsgiverperioden ifbm. graviditet
            """.trimIndent(),
            tema = "SYK",
            behandlingstype = digitalKravBehandingsType,
            oppgavetype = "BEH_SAK",
            behandlingstema = fritakAGPBehandingsTema,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = LocalDate.now().plusDays(7),
            prioritet = "NORM"
        )

        return runBlocking { oppgaveKlient.opprettOppgave(request, UUID.randomUUID().toString()).id.toString() }
    }

    data class JobbData(val id: UUID)

}