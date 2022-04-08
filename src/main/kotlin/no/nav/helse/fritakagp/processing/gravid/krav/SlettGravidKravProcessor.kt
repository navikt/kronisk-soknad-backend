package no.nav.helse.fritakagp.processing.gravid.krav

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.AvsenderMottaker
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.Bruker
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.Dokument
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokumentVariant
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.IdType
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostRequest
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.Journalposttype
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OPPGAVETYPE_FORDELINGSOPPGAVE
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlIdent
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.domain.generereSlettGravidKravBeskrivelse
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.service.BehandlendeEnhetService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class SlettGravidKravProcessor(
    private val gravidKravRepo: GravidKravRepository,
    private val dokarkivKlient: DokarkivKlient,
    private val oppgaveKlient: OppgaveKlient,
    private val pdlClient: PdlClient,
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
    private val pdfGenerator: GravidKravPDFGenerator,
    private val om: ObjectMapper,
    private val bucketStorage: BucketStorage,
    private val brregClient: BrregClient,
    private val behandlendeEnhetService: BehandlendeEnhetService
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "slett-gravid-krav"
        val dokumentasjonBrevkode = "annuler_krav_om_fritak_fra_agp_dokumentasjon"
    }
    override val type: String get() = JOB_TYPE

    val digitalKravBehandingsType = "ae0121"
    val fritakAGPBehandingsTema = "ab0200"

    val log = LoggerFactory.getLogger(SlettGravidKravProcessor::class.java)

    /**
     * Prosesserer sletting av  gravidkrav; journalfører og oppretter en oppgave for saksbehandler.
     * Jobbdataene forventes å være en UUID for et krav som skal prosesseres.
     */
    override fun prosesser(jobb: Bakgrunnsjobb) {
        val krav = getOrThrow(jobb)
        log.info("Sletter krav ${krav.id}")
        try {
            journalførSletting(krav)
            krav.oppgaveId = opprettOppgave(krav)
        } finally {
            updateAndLogOnFailure(krav)
        }
    }

    private fun getOrThrow(jobb: Bakgrunnsjobb): GravidKrav {
        val jobbData = om.readValue<GravidKravProcessor.JobbData>(jobb.data)
        val krav = gravidKravRepo.getById(jobbData.id)
        requireNotNull(krav, { "Jobben indikerte et krav med id ${jobb.data} men den kunne ikke finnes" })
        return krav
    }

    override fun stoppet(jobb: Bakgrunnsjobb) {
        val krav = getOrThrow(jobb)
        val oppgaveId = opprettFordelingsOppgave(krav)
        log.warn("Jobben ${jobb.uuid} feilet permanenet og resulterte i fordelignsoppgave $oppgaveId")
    }

    private fun updateAndLogOnFailure(krav: GravidKrav) {
        try {
            gravidKravRepo.update(krav)
        } catch (e: Exception) {
            throw RuntimeException("Feilet i å oppdatere slettet krav ${krav.id} etter at en ekstern operasjon har blitt utført. JournalpostID: ${krav.journalpostId} OppgaveID: ${krav.oppgaveId}", e)
        }
    }

    fun journalførSletting(krav: GravidKrav): String {
        val journalfoeringsTittel = "Annuller krav om fritak fra arbeidsgiverperioden ifbm graviditet"
        val response = dokarkivKlient.journalførDokument(
            JournalpostRequest(
                tittel = journalfoeringsTittel,
                journalposttype = Journalposttype.INNGAAENDE,
                kanal = "NAV_NO",
                bruker = Bruker(krav.identitetsnummer, IdType.FNR),
                eksternReferanseId = "${krav.id}-annul",
                avsenderMottaker = AvsenderMottaker(
                    id = krav.sendtAv,
                    idType = IdType.FNR,
                    navn = krav.virksomhetsnavn ?: "Arbeidsgiver Ukjent"
                ),
                dokumenter = createDocuments(krav, journalfoeringsTittel),
                datoMottatt = krav.opprettet.toLocalDate()
            ),
            true, UUID.randomUUID().toString()

        )

        log.debug("Journalført ${krav.id} med ref ${response.journalpostId}")
        return response.journalpostId
    }

    private fun createDocuments(
        krav: GravidKrav,
        journalfoeringsTittel: String
    ): List<Dokument> {
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagSlettingPDF(krav))
        val jsonOrginalDokument = Base64.getEncoder().encodeToString(om.writeValueAsBytes(krav))
        val dokumentListe = mutableListOf(
            Dokument(
                dokumentVarianter = listOf(
                    DokumentVariant(
                        fysiskDokument = base64EnkodetPdf,
                    ),
                    DokumentVariant(
                        filtype = "JSON",
                        fysiskDokument = jsonOrginalDokument,
                        variantFormat = "ORIGINAL"
                    )
                ),
                brevkode = dokumentasjonBrevkode,
                tittel = journalfoeringsTittel,
            )
        )

        bucketStorage.getDocAsString(krav.id)?.let {
            dokumentListe.add(
                Dokument(
                    dokumentVarianter = listOf(
                        DokumentVariant(
                            fysiskDokument = it.base64Data,
                            filtype = if (it.extension == "jpg") "JPEG" else it.extension.uppercase()
                        ),
                        DokumentVariant(
                            filtype = "JSON",
                            fysiskDokument = jsonOrginalDokument,
                            variantFormat = "ORIGINAL"
                        )
                    ),
                    brevkode = GravidKravProcessor.dokumentasjonBrevkode,
                    tittel = "Helsedokumentasjon",
                )
            )
        }

        return dokumentListe
    }

    fun opprettOppgave(krav: GravidKrav): String {

        val aktoerId = pdlClient.fullPerson(krav.identitetsnummer)?.hentIdenter?.trekkUtIdent(PdlIdent.PdlIdentGruppe.AKTORID)
        val enhetsNr = behandlendeEnhetService.hentBehandlendeEnhet(krav.identitetsnummer, krav.id.toString())
        requireNotNull(aktoerId) { "Fant ikke AktørID for fnr i ${krav.id}" }
        log.info("Fant aktørid")
        val request = OpprettOppgaveRequest(
            tildeltEnhetsnr = enhetsNr,
            aktoerId = aktoerId,
            journalpostId = krav.journalpostId,
            beskrivelse = generereSlettGravidKravBeskrivelse(krav, "Annullering av refusjonskrav ifbm sykdom i aprbeidsgiverperioden med fritak fra arbeidsgiverperioden grunnet kronisk sykdom."),
            tema = "SYK",
            behandlingstype = digitalKravBehandingsType,
            oppgavetype = "BEH_REF",
            behandlingstema = fritakAGPBehandingsTema,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = LocalDate.now().plusDays(7),
            prioritet = "NORM"
        )

        return runBlocking { oppgaveKlient.opprettOppgave(request, UUID.randomUUID().toString()).id.toString() }
    }

    fun opprettFordelingsOppgave(krav: GravidKrav): String {
        val aktoerId = pdlClient.fullPerson(krav.identitetsnummer)?.hentIdenter?.trekkUtIdent(PdlIdent.PdlIdentGruppe.AKTORID)
        val enhetsNr = behandlendeEnhetService.hentBehandlendeEnhet(krav.identitetsnummer, krav.id.toString())
        requireNotNull(aktoerId) { "Fant ikke AktørID for fnr i ${krav.id}" }

        val request = OpprettOppgaveRequest(
            tildeltEnhetsnr = enhetsNr,
            aktoerId = aktoerId,
            journalpostId = krav.journalpostId,
            beskrivelse = generereSlettGravidKravBeskrivelse(krav, "Fordelingsoppgave for annullering av refusjonskrav ifbm sykdom i aprbeidsgiverperioden med fritak fra arbeidsgiverperioden grunnet kronisk sykdom."),
            tema = "SYK",
            behandlingstype = digitalKravBehandingsType,
            oppgavetype = OPPGAVETYPE_FORDELINGSOPPGAVE,
            behandlingstema = fritakAGPBehandingsTema,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = LocalDate.now().plusDays(7),
            prioritet = "NORM"
        )

        return runBlocking { oppgaveKlient.opprettOppgave(request, UUID.randomUUID().toString()).id.toString() }
    }
}
