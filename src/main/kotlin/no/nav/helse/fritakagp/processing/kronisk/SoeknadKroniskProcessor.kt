package no.nav.helse.fritakagp.processing.kronisk

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.*
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlIdent
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helse.fritakagp.domain.SoeknadKronisk
import no.nav.helse.fritakagp.gcp.BucketStorage
import no.nav.security.mock.oauth2.http.objectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class SoeknadKroniskProcessor(
    private val kroniskSoeknadRepo: KroniskSoeknadRepository,
    private val dokarkivKlient: DokarkivKlient,
    private val oppgaveKlient: OppgaveKlient,
    private val pdlClient: PdlClient,
    private val pdfGenerator: KroniskSoeknadPDFGenerator,
    private val om: ObjectMapper,
    private val bucketStorage: BucketStorage
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "PROC_Kronisk_søk"
        val dokumentasjonBrevkode = "soeknad_om_fritak_fra_agp_dokumentasjon"
    }

    val digitalSoeknadBehandingsType = "ae0227"
    val fritakAGPBehandingsTema = "ab0338"

    val log = LoggerFactory.getLogger(SoeknadKroniskProcessor::class.java)

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return LocalDateTime.now().plusHours(3)
    }

    /**
     * Prosesserer en kronisksøknad; journalfører søknaden og oppretter en oppgave for saksbehandler.
     * Jobbdataene forventes å være en UUID for en søknad som skal prosesseres.
     */
    override fun prosesser(jobbDataString: String) {
        val jobbData = om.readValue<JobbData>(jobbDataString)
        val soeknad = kroniskSoeknadRepo.getById(jobbData.id)
        requireNotNull(soeknad, { "Jobben indikerte en søknad med id $jobbData men den kunne ikke finnes" })

        try {
            if (soeknad.journalpostId == null) {
                soeknad.journalpostId = journalfør(soeknad)
            }

            bucketStorage.deleteDoc(soeknad.id)

            if (soeknad.oppgaveId == null) {
                soeknad.oppgaveId = opprettOppgave(soeknad)
            }
        } finally {
            updateAndLogOnFailure(soeknad)
        }
    }

    private fun updateAndLogOnFailure(soeknad: SoeknadKronisk) {
        try {
            kroniskSoeknadRepo.update(soeknad)
        } catch (e: Exception) {
            log.error("Feilet i å lagre ${soeknad.id} etter at en ekstern operasjon har blitt utført. JournalpostID: ${soeknad.journalpostId} OppgaveID: ${soeknad.oppgaveId}")
            throw e
        }
    }

    fun journalfør(soeknad: SoeknadKronisk): String {
        val journalfoeringsTittel = "Søknad om fritak fra arbeidsgiverperioden ifbm kronisk lidelse"
        val pdlResponse = pdlClient.personNavn(soeknad.sendtAv)?.navn?.firstOrNull()
        val innsenderNavn = if (pdlResponse != null) "${pdlResponse.fornavn} ${pdlResponse.etternavn}" else "Ukjent"

        val response = dokarkivKlient.journalførDokument(
            JournalpostRequest(
                tittel = journalfoeringsTittel,
                journalposttype = Journalposttype.INNGAAENDE,
                kanal = "NAV_NO",
                bruker = Bruker(soeknad.fnr, IdType.FNR),
                eksternReferanseId = soeknad.id.toString(),
                avsenderMottaker = AvsenderMottaker(
                    id = soeknad.sendtAv,
                    idType = IdType.FNR,
                    navn = innsenderNavn
                ),
                dokumenter = createDocuments(soeknad, journalfoeringsTittel),
                datoMottatt = soeknad.opprettet.toLocalDate()
            ), true, UUID.randomUUID().toString()

        )

        log.debug("Journalført ${soeknad.id} med ref ${response.journalpostId}")
        return response.journalpostId
    }

    private fun createDocuments(
        soeknad: SoeknadKronisk,
        journalfoeringsTittel: String
    ): List<Dokument> {
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagPDF(soeknad))


        val dokumentListe = mutableListOf(
            Dokument(
                dokumentVarianter = listOf(
                    DokumentVariant(
                        fysiskDokument = base64EnkodetPdf
                    )
                ),
                brevkode = "soeknad_om_fritak_fra_agp_kronisk",
                tittel = journalfoeringsTittel,
            )
        )

        bucketStorage.getDocAsString(soeknad.id)?.let {
            dokumentListe.add(
                Dokument(
                    dokumentVarianter = listOf(
                        DokumentVariant(
                            fysiskDokument = it.base64Data,
                            filtype = if (it.extension == "jpg") "JPEG" else it.extension.toUpperCase()
                        )
                    ),
                    brevkode = Companion.dokumentasjonBrevkode,
                    tittel = "Helsedokumentasjon",
                )
            )
        }

        return dokumentListe
    }

    fun opprettOppgave(soeknad: SoeknadKronisk): String {
        val aktoerId = pdlClient.fullPerson(soeknad.fnr)?.hentIdenter?.trekkUtIdent(PdlIdent.PdlIdentGruppe.AKTORID)
        requireNotNull(aktoerId, { "Fant ikke AktørID for fnr i ${soeknad.id}" })

        val request = OpprettOppgaveRequest(
            aktoerId = aktoerId,
            journalpostId = soeknad.journalpostId,
            beskrivelse = """
                Søknad om fritak fra arbeidsgiverperioden ifbm kronisk lidelse
            """.trimIndent(),
            tema = "SYK",
            behandlingstype = digitalSoeknadBehandingsType,
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