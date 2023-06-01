package no.nav.helse.fritakagp.processing.kronisk.krav

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
import no.nav.helse.fritakagp.KroniskKravMetrics
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.domain.generereKroniskKravBeskrivelse
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.journalførOgFerdigstillDokument
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor.Jobbdata.SkjemaType
import no.nav.helse.fritakagp.service.BehandlendeEnhetService
import no.nav.helsearbeidsgiver.utils.log.logger
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

class KroniskKravProcessor(
    private val kroniskKravRepo: KroniskKravRepository,
    private val dokarkivKlient: DokarkivKlient,
    private val oppgaveKlient: OppgaveKlient,
    private val pdlClient: PdlClient,
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
    private val pdfGenerator: KroniskKravPDFGenerator,
    private val om: ObjectMapper,
    private val bucketStorage: BucketStorage,
    private val brregClient: BrregClient,
    private val behandlendeEnhetService: BehandlendeEnhetService,
    private val robotiseringToggle: Boolean = false
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "kronisk-krav-formidling"
        val dokumentasjonBrevkode = "krav_om_fritak_fra_agp_dokumentasjon"
    }

    override val type: String get() = JOB_TYPE

    val digitalKravBehandingsType = "ae0121"
    val fritakAGPBehandingsTema = "ab0200"

    private val logger = this.logger()

    /**
     * Prosesserer et kroniskkrav; journalfører kravet og oppretter en oppgave for saksbehandler.
     * Jobbdataene forventes å være en UUID for et krav som skal prosesseres.
     */
    override fun prosesser(jobb: Bakgrunnsjobb) {
        val krav = getOrThrow(jobb)
        logger.info("Prosesserer krav ${krav.id}")

        try {
            if (krav.virksomhetsnavn == null) {
                runBlocking {
                    krav.virksomhetsnavn = brregClient.getVirksomhetsNavn(krav.virksomhetsnummer)
                    logger.info("Slo opp virksomhet")
                }
            }
            if (krav.journalpostId == null) {
                krav.journalpostId = journalfør(krav)
                KroniskKravMetrics.tellJournalfoert()
            }

            bucketStorage.deleteDoc(krav.id)
            logger.info("Slettet eventuelle vedlegg")

            if (krav.oppgaveId == null) {
                krav.oppgaveId = opprettOppgave(krav)
                logger.info("Oppgave opprettet med id ${krav.oppgaveId}")
                KroniskKravMetrics.tellOppgaveOpprettet()
            }
            bakgrunnsjobbRepo.save(
                Bakgrunnsjobb(
                    maksAntallForsoek = 10,
                    data = om.writeValueAsString(KroniskKravKafkaProcessor.JobbData(krav.id)),
                    type = KroniskKravKafkaProcessor.JOB_TYPE
                )
            )
            bakgrunnsjobbRepo.save(
                Bakgrunnsjobb(
                    maksAntallForsoek = 10,
                    data = om.writeValueAsString(BrukernotifikasjonProcessor.Jobbdata(krav.id, SkjemaType.KroniskKrav)),
                    type = BrukernotifikasjonProcessor.JOB_TYPE
                )
            )
        } finally {
            updateAndLogOnFailure(krav)
        }
    }

    private fun getOrThrow(jobb: Bakgrunnsjobb): KroniskKrav {
        val jobbData = om.readValue<JobbData>(jobb.data)
        val krav = kroniskKravRepo.getById(jobbData.id)
        requireNotNull(krav, { "Jobben indikerte et krav med id ${jobb.data} men den kunne ikke finnes" })
        return krav
    }

    /**
     * Når vi gir opp, opprette en fordelingsoppgave til saksbehandler
     */
    override fun stoppet(jobb: Bakgrunnsjobb) {
        val krav = getOrThrow(jobb)
        val oppgaveId = opprettFordelingsOppgave(krav)
        logger.warn("Jobben ${jobb.uuid} feilet permanenet og resulterte i fordelignsoppgave $oppgaveId")
    }

    private fun updateAndLogOnFailure(krav: KroniskKrav) {
        try {
            kroniskKravRepo.update(krav)
        } catch (e: Exception) {
            throw RuntimeException("Feilet i å lagre ${krav.id} etter at en ekstern operasjon har blitt utført. JournalpostID: ${krav.journalpostId} OppgaveID: ${krav.oppgaveId}", e)
        }
    }

    fun journalfør(krav: KroniskKrav): String {
        val journalpostId = dokarkivKlient.journalførOgFerdigstillDokument(
            JournalpostRequest(
                tittel = KroniskKrav.tittel,
                journalposttype = Journalposttype.INNGAAENDE,
                kanal = "NAV_NO",
                bruker = Bruker(krav.identitetsnummer, IdType.FNR),
                eksternReferanseId = krav.id.toString(),
                avsenderMottaker = AvsenderMottaker(
                    id = krav.virksomhetsnummer,
                    idType = IdType.ORGNR,
                    navn = krav.virksomhetsnavn ?: "Ukjent arbeidsgiver"
                ),
                dokumenter = createDocuments(krav, KroniskKrav.tittel),
                datoMottatt = krav.opprettet.toLocalDate()
            ),
            UUID.randomUUID().toString(),
            om,
            logger
        )

        logger.info("Journalført ${krav.id} med ref $journalpostId")
        return journalpostId
    }

    private fun createDocuments(
        krav: KroniskKrav,
        journalfoeringsTittel: String
    ): List<Dokument> {
        val base64EnkodetPdf = Base64.getEncoder().encodeToString(pdfGenerator.lagPDF(krav))
        val jsonOrginalDokument = Base64.getEncoder().encodeToString(om.writeValueAsBytes(krav))
        val dokumentListe = mutableListOf(
            Dokument(
                dokumentVarianter = listOf(
                    DokumentVariant(
                        fysiskDokument = base64EnkodetPdf
                    ),
                    DokumentVariant(
                        filtype = "JSON",
                        fysiskDokument = jsonOrginalDokument,
                        variantFormat = "ORIGINAL"
                    )
                ),
                brevkode = "krav_om_fritak_fra_agp_kronisk",
                tittel = journalfoeringsTittel
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
                    brevkode = dokumentasjonBrevkode,
                    tittel = "Helsedokumentasjon"
                )
            )
        }

        return dokumentListe
    }

    fun opprettOppgave(krav: KroniskKrav): String {
        val aktoerId = pdlClient.fullPerson(krav.identitetsnummer)?.hentIdenter?.trekkUtIdent(PdlIdent.PdlIdentGruppe.AKTORID)
        val enhetsNr = behandlendeEnhetService.hentBehandlendeEnhet(krav.identitetsnummer, krav.id.toString())
        requireNotNull(aktoerId) { "Fant ikke AktørID for fnr i ${krav.id}" }
        logger.info("Fant aktørid")
        val beskrivelse = if (robotiseringToggle) om.writeValueAsString(krav.toKravListeForOppgave()[0]) else generereKroniskKravBeskrivelse(krav, KroniskKrav.tittel)
        val oppgaveType = if (robotiseringToggle) "ROB_BEH" else "BEH_REF"
        val request = OpprettOppgaveRequest(
            tildeltEnhetsnr = enhetsNr,
            aktoerId = aktoerId,
            journalpostId = krav.journalpostId,
            beskrivelse = beskrivelse,
            tema = "SYK",
            behandlingstype = digitalKravBehandingsType,
            oppgavetype = oppgaveType,
            behandlingstema = fritakAGPBehandingsTema,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = LocalDate.now().plusDays(7),
            prioritet = "NORM"
        )

        return runBlocking { oppgaveKlient.opprettOppgave(request, UUID.randomUUID().toString()).id.toString() }
    }

    fun opprettFordelingsOppgave(krav: KroniskKrav): String {
        val aktoerId = pdlClient.fullPerson(krav.identitetsnummer)?.hentIdenter?.trekkUtIdent(PdlIdent.PdlIdentGruppe.AKTORID)
        val enhetsNr = behandlendeEnhetService.hentBehandlendeEnhet(krav.identitetsnummer, krav.id.toString())
        requireNotNull(aktoerId) { "Fant ikke AktørID for fnr i ${krav.id}" }

        val request = OpprettOppgaveRequest(
            tildeltEnhetsnr = enhetsNr,
            aktoerId = aktoerId,
            journalpostId = krav.journalpostId,
            beskrivelse = generereKroniskKravBeskrivelse(krav, "Fordelingsoppgave for ${KroniskKrav.tittel}"),
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

    data class JobbData(val id: UUID)
}
