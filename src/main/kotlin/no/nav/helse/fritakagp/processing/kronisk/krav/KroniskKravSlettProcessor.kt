package no.nav.helse.fritakagp.processing.kronisk.krav

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.OPPGAVETYPE_FORDELINGSOPPGAVE
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.OpprettOppgaveRequest
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.domain.generereSlettKroniskKravBeskrivelse
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.service.PdlService
import no.nav.helsearbeidsgiver.dokarkiv.DokArkivClient
import no.nav.helsearbeidsgiver.dokarkiv.domene.Avsender
import no.nav.helsearbeidsgiver.dokarkiv.domene.Dokument
import no.nav.helsearbeidsgiver.dokarkiv.domene.DokumentVariant
import no.nav.helsearbeidsgiver.dokarkiv.domene.GjelderPerson
import no.nav.helsearbeidsgiver.utils.log.logger
import java.time.LocalDate
import java.util.*

class KroniskKravSlettProcessor(
    private val kroniskKravRepo: KroniskKravRepository,
    private val dokarkivKlient: DokArkivClient,
    private val oppgaveKlient: OppgaveKlient,
    private val pdlService: PdlService,
    private val pdfGenerator: KroniskKravPDFGenerator,
    private val om: ObjectMapper,
    private val bucketStorage: BucketStorage
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "slett-kronisk-krav"
        val dokumentasjonBrevkode = "annuller_krav_om_fritak_fra_agp_dokumentasjon"
    }

    override val type: String get() = JOB_TYPE

    val digitalKravBehandingsType = "ae0121"
    val fritakAGPBehandingsTema = "ab0200"

    private val logger = this.logger()

    /**
     * Prosesserer sletting av kroniskkrav; journalfører og oppretter en oppgave for saksbehandler.
     * Jobbdataene forventes å være en UUID for et krav som skal prosesseres.
     */
    override fun prosesser(jobb: Bakgrunnsjobb) {
        val krav = getOrThrow(jobb)
        logger.info("Sletter krav ${krav.id}")
        try {
            krav.sletteJournalpostId = journalførSletting(krav)
            krav.sletteOppgaveId = opprettOppgave(krav)
        } finally {
            updateAndLogOnFailure(krav)
        }
    }

    private fun getOrThrow(jobb: Bakgrunnsjobb): KroniskKrav {
        val jobbData = om.readValue<KroniskKravProcessor.JobbData>(jobb.data)
        val krav = kroniskKravRepo.getById(jobbData.id)
        requireNotNull(krav, { "Jobben indikerte et krav med id ${jobb.data} men den kunne ikke finnes" })
        return krav
    }

    override fun stoppet(jobb: Bakgrunnsjobb) {
        val krav = getOrThrow(jobb)
        val oppgaveId = opprettFordelingsOppgave(krav)
        logger.warn("Jobben ${jobb.uuid} feilet permanent og resulterte i fordelingsoppgave $oppgaveId")
    }

    private fun updateAndLogOnFailure(krav: KroniskKrav) {
        try {
            kroniskKravRepo.update(krav)
        } catch (e: Exception) {
            throw RuntimeException("Feilet i å oppdatere slettet krav ${krav.id} etter at en ekstern operasjon har blitt utført. JournalpostID: ${krav.journalpostId} OppgaveID: ${krav.oppgaveId}", e)
        }
    }

    fun journalførSletting(krav: KroniskKrav): String {
        val journalfoeringsTittel = "Annuller ${KroniskKrav.tittel}"
        val id = runBlocking {
            val journalpostId = dokarkivKlient.opprettOgFerdigstillJournalpost(
                tittel = journalfoeringsTittel,
                gjelderPerson = GjelderPerson(krav.identitetsnummer),
                avsender = Avsender.Organisasjon(krav.virksomhetsnummer, krav.virksomhetsnavn ?: "Ukjent arbeidsgiver"),
                datoMottatt = krav.opprettet.toLocalDate(),
                dokumenter = createDocuments(krav, journalfoeringsTittel),
                eksternReferanseId = "${krav.id}-annul",
                callId = UUID.randomUUID().toString()
            )
            logger.debug("Journalført ${krav.id} med ref $journalpostId")
            return@runBlocking journalpostId.journalpostId
        }
        return id
    }

    private fun createDocuments(
        krav: KroniskKrav,
        journalfoeringsTittel: String
    ): List<Dokument> {
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagSlettingPDF(krav))
        val jsonOrginalDokument = Base64.getEncoder().encodeToString(om.writeValueAsBytes(krav))
        val dokumentListe = mutableListOf(
            Dokument(
                dokumentVarianter = listOf(
                    DokumentVariant(
                        fysiskDokument = base64EnkodetPdf,
                        filtype = "PDF",
                        variantFormat = "ARKIV",
                        filnavn = null
                    ),
                    DokumentVariant(
                        filtype = "JSON",
                        fysiskDokument = jsonOrginalDokument,
                        variantFormat = "ORIGINAL",
                        filnavn = null
                    )
                ),
                brevkode = dokumentasjonBrevkode,
                tittel = journalfoeringsTittel
            )
        )

        bucketStorage.getDocAsString(krav.id)?.let {
            dokumentListe.add(
                Dokument(
                    dokumentVarianter = listOf(
                        DokumentVariant(
                            fysiskDokument = it.base64Data,
                            filtype = it.extension.uppercase(),
                            variantFormat = "ARKIV",
                            filnavn = null
                        ),
                        DokumentVariant(
                            filtype = "JSON",
                            fysiskDokument = jsonOrginalDokument,
                            variantFormat = "ORIGINAL",
                            filnavn = null
                        )
                    ),
                    brevkode = KroniskKravProcessor.dokumentasjonBrevkode,
                    tittel = "Helsedokumentasjon"
                )
            )
        }

        return dokumentListe
    }

    fun opprettOppgave(krav: KroniskKrav): String {
        val aktoerId = pdlService.hentAktoerId(krav.identitetsnummer)
        requireNotNull(aktoerId) { "Fant ikke AktørID for fnr i ${krav.id}" }
        logger.info("Fant aktørid")
        val request = OpprettOppgaveRequest(
            aktoerId = aktoerId,
            journalpostId = krav.journalpostId,
            beskrivelse = generereSlettKroniskKravBeskrivelse(krav, "Annullering av ${KroniskKrav.tittel}"),
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

    fun opprettFordelingsOppgave(krav: KroniskKrav): String {
        val aktoerId = pdlService.hentAktoerId(krav.identitetsnummer)
        requireNotNull(aktoerId) { "Fant ikke AktørID for fnr i ${krav.id}" }

        val request = OpprettOppgaveRequest(
            aktoerId = aktoerId,
            journalpostId = krav.journalpostId,
            beskrivelse = generereSlettKroniskKravBeskrivelse(krav, "Fordelingsoppgave for annullering av ${KroniskKrav.tittel}"),
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
