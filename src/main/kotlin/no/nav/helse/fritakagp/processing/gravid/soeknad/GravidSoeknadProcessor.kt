package no.nav.helse.fritakagp.processing.gravid.soeknad

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.features.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.*
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OPPGAVETYPE_FORDELINGSOPPGAVE
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlIdent
import no.nav.helse.fritakagp.GravidSoeknadMetrics
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.domain.generereGravidSoeknadBeskrivelse
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor.Jobbdata.SkjemaType.GravidSøknad
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class GravidSoeknadProcessor(
    private val gravidSoeknadRepo: GravidSoeknadRepository,
    private val dokarkivKlient: DokarkivKlient,
    private val oppgaveKlient: OppgaveKlient,
    private val pdlClient: PdlClient,
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
    private val pdfGenerator: GravidSoeknadPDFGenerator,
    private val om: ObjectMapper,
    private val bucketStorage: BucketStorage,
    private val brregClient: BrregClient
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "gravid-søknad-formidling"
        val dokumentasjonBrevkode = "soeknad_om_fritak_fra_agp_dokumentasjon"
    }
    override val type: String get() = JOB_TYPE

    val digitalSoeknadBehandingsType = "ae0227"
    val fritakAGPBehandingsTema = "ab0338"

    val log = LoggerFactory.getLogger(GravidSoeknadProcessor::class.java)

    /**
     * Prosesserer en gravidsøknad; journalfører søknaden og oppretter en oppgave for saksbehandler.
     * Jobbdataene forventes å være en UUID for en søknad som skal prosesseres.
     */
    override fun prosesser(jobb: Bakgrunnsjobb) {
        val jobbData = om.readValue<JobbData>(jobb.data)
        val soeknad = gravidSoeknadRepo.getById(jobbData.id)
        requireNotNull(soeknad, { "Jobben indikerte en søknad med id ${jobb.data} men den kunne ikke finnes" })

        try {
            if (soeknad.virksomhetsnavn == null) {
                runBlocking {
                    soeknad.virksomhetsnavn = brregClient.getVirksomhetsNavn(soeknad.virksomhetsnummer)
                }
            }
            if (soeknad.journalpostId == null) {
                soeknad.journalpostId = journalfør(soeknad)
                GravidSoeknadMetrics.tellJournalfoert()
            }

            bucketStorage.deleteDoc(soeknad.id)

            if (soeknad.oppgaveId == null) {
                soeknad.oppgaveId = opprettOppgave(soeknad)
                GravidSoeknadMetrics.tellOppgaveOpprettet()
            }

            bakgrunnsjobbRepo.save(
                Bakgrunnsjobb(
                    maksAntallForsoek = 10,
                    data = om.writeValueAsString(GravidSoeknadKafkaProcessor.JobbData(soeknad.id)),
                    type = GravidSoeknadKafkaProcessor.JOB_TYPE
                )
            )
            bakgrunnsjobbRepo.save(
                Bakgrunnsjobb(
                    maksAntallForsoek = 10,
                    data = om.writeValueAsString(BrukernotifikasjonProcessor.Jobbdata(soeknad.id, GravidSøknad)),
                    type = BrukernotifikasjonProcessor.JOB_TYPE
                )
            )

        } finally {
            updateAndLogOnFailure(soeknad)
        }
    }

    private fun updateAndLogOnFailure(soeknad: GravidSoeknad) {
        try {
            gravidSoeknadRepo.update(soeknad)
        } catch (e: Exception) {
            throw RuntimeException("Feilet i å lagre ${soeknad.id} etter at en ekstern operasjon har blitt utført. JournalpostID: ${soeknad.journalpostId} OppgaveID: ${soeknad.oppgaveId}", e)
        }
    }

    fun journalfør(soeknad: GravidSoeknad): String {
        val journalfoeringsTittel = "Søknad om fritak fra arbeidsgiverperioden ifbm graviditet"

        val response = dokarkivKlient.journalførDokument(
            JournalpostRequest(
                tittel = journalfoeringsTittel,
                journalposttype = Journalposttype.INNGAAENDE,
                kanal = "NAV_NO",
                bruker = Bruker(soeknad.identitetsnummer, IdType.FNR),
                eksternReferanseId = soeknad.id.toString(),
                avsenderMottaker = AvsenderMottaker(
                    id = soeknad.sendtAv,
                    idType = IdType.FNR,
                    navn = soeknad.virksomhetsnavn ?: "Ukjent arbeidsgiver"
                ),
                dokumenter = createDocuments(soeknad, journalfoeringsTittel),
                datoMottatt = soeknad.opprettet.toLocalDate()
            ), true, UUID.randomUUID().toString()

        )

        log.debug("Journalført ${soeknad.id} med ref ${response.journalpostId}")
        return response.journalpostId
    }

    /**
     * Når vi gir opp, opprette en fordelingsoppgave til saksbehandler
     */
    override fun stoppet(jobb: Bakgrunnsjobb) {
        val soeknad = getSoeknadOrThrow(jobb)
        val oppgaveId = opprettFordelingsOppgave(soeknad)
        log.warn("Jobben ${jobb.uuid} feilet permanenet og resulterte i fordelignsoppgave $oppgaveId")
    }

    private fun getSoeknadOrThrow(jobb: Bakgrunnsjobb): GravidSoeknad {
        val jobbData = om.readValue<JobbData>(jobb.data)
        val soeknad = gravidSoeknadRepo.getById(jobbData.id)
        requireNotNull(soeknad, { "Jobben indikerte en søknad med id ${jobb.data} men den kunne ikke finnes" })
        return soeknad
    }


    private fun createDocuments(
        soeknad: GravidSoeknad,
        journalfoeringsTittel: String
    ): List<Dokument> {
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagPDF(soeknad))
        val jsonOrginalDokument = Base64.getEncoder().encodeToString(om.writeValueAsBytes(soeknad))
        val dokumentListe = mutableListOf(
            Dokument(
                dokumentVarianter = listOf(
                    DokumentVariant(
                        fysiskDokument = base64EnkodetPdf
                    )
                ),
                brevkode = "soeknad_om_fritak_fra_agp_gravid",
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
                        ),
                            DokumentVariant(
                                    variantFormat = "ORIGINAL",
                                    fysiskDokument = jsonOrginalDokument,
                                    filtype = "JSON"
                            )
                    ),
                    brevkode = dokumentasjonBrevkode,
                    tittel = "Helsedokumentasjon",
                )
            )
        }

        return dokumentListe
    }

    fun opprettOppgave(soeknad: GravidSoeknad): String {
        val aktoerId = pdlClient.fullPerson(soeknad.identitetsnummer)?.hentIdenter?.trekkUtIdent(PdlIdent.PdlIdentGruppe.AKTORID)
        requireNotNull(aktoerId) { "Fant ikke AktørID for fnr i ${soeknad.id}" }

        val request = OpprettOppgaveRequest(
            aktoerId = aktoerId,
            journalpostId = soeknad.journalpostId,
            beskrivelse = generereGravidSoeknadBeskrivelse(soeknad, "Søknad om fritak fra arbeidsgiverperioden ifbm. graviditet"),
            tema = "SYK",
            behandlingstype = digitalSoeknadBehandingsType,
            oppgavetype = "BEH_SAK",
            behandlingstema = fritakAGPBehandingsTema,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = LocalDate.now().plusDays(7),
            prioritet = "NORM"
        )

        return runBlocking {
            try {
                oppgaveKlient.opprettOppgave(request, UUID.randomUUID().toString()).id.toString()
            } catch(ex:Exception) {
                var error = """Response fra opprettOppgave:
                    | message : ${ex.message}                    
                    | cause : ${ex.cause}
                    | suppressed : ${strex(ex.suppressed)}
                    """

                log.error(error)
                throw ex
            }
        }
    }
    fun strex(exes : Array<Throwable>) : String{
        var err : String  = ""
        for (e in exes)
            err += e.message + " ||||| "

        return err
    }
    fun opprettFordelingsOppgave(soeknad: GravidSoeknad): String {
        val aktoerId = pdlClient.fullPerson(soeknad.identitetsnummer)?.hentIdenter?.trekkUtIdent(PdlIdent.PdlIdentGruppe.AKTORID)
        requireNotNull(aktoerId) { "Fant ikke AktørID for fnr i ${soeknad.id}" }

        val request = OpprettOppgaveRequest(
            aktoerId = aktoerId,
            journalpostId = soeknad.journalpostId,
            beskrivelse = generereGravidSoeknadBeskrivelse(soeknad, "Fordelingsoppgave for søknad om fritak fra arbeidsgiverperioden grunnet gravid sykdom."),
            tema = "SYK",
            behandlingstype = digitalSoeknadBehandingsType,
            oppgavetype = OPPGAVETYPE_FORDELINGSOPPGAVE,
            behandlingstema = fritakAGPBehandingsTema,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = LocalDate.now().plusDays(7),
            prioritet = "NORM"
        )

        return runBlocking { oppgaveKlient.opprettOppgave(request, UUID.randomUUID().toString()).id.toString() }
    }


    data class JobbData(val id: UUID)

}