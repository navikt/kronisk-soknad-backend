package no.nav.helse.fritakagp.processing.gravid.krav

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.OpprettOppgaveRequest
import no.nav.helse.fritakagp.GravidKravMetrics
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.service.BehandlendeEnhetService
import no.nav.helse.fritakagp.service.PdlService
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class OpprettRobotOppgaveGravidProcessor(
    private val gravidKravRepo: GravidKravRepository,
    private val oppgaveKlient: OppgaveKlient,
    private val pdlService: PdlService,
    private val om: ObjectMapper,
    private val behandlendeEnhetService: BehandlendeEnhetService
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "gravid-krav-robot-oppgave"
    }

    override val type: String get() = JOB_TYPE

    val digitalKravBehandingsType = "ae0121"
    val fritakAGPBehandingsTema = "ab0200"

    private val logger = this.logger()
    override fun prosesser(jobb: Bakgrunnsjobb) {
        val krav = getOrThrow(jobb)
        logger.info("Prosesserer krav ${krav.id}")

        try {
            if (krav.oppgaveId == null) {
                krav.oppgaveId = opprettOppgave(krav)
                logger.info("Robot Oppgave opprettet med id ${krav.oppgaveId}")
                GravidKravMetrics.tellOppgaveOpprettet()
            }
        } finally {
            updateAndLogOnFailure(krav)
        }
    }

    private fun getOrThrow(jobb: Bakgrunnsjobb): GravidKrav {
        val jobbData = om.readValue<JobbData>(jobb.data)
        val krav = gravidKravRepo.getById(jobbData.id)
        requireNotNull(krav, { "Jobben indikerte et krav med id ${jobb.data} men den kunne ikke finnes" })
        return krav
    }

    override fun stoppet(jobb: Bakgrunnsjobb) {
        logger.warn("Jobben ${jobb.uuid} feilet permanent")
    }

    private fun updateAndLogOnFailure(krav: GravidKrav) {
        try {
            gravidKravRepo.update(krav)
        } catch (e: Exception) {
            throw RuntimeException("Feilet i å lagre ${krav.id} etter at en ekstern operasjon har blitt utført. JournalpostID: ${krav.journalpostId} OppgaveID: ${krav.oppgaveId}", e)
        }
    }

    fun opprettOppgave(krav: GravidKrav): String {
        val aktoerId = pdlService.hentAktoerId(krav.identitetsnummer)
        val enhetsNr = behandlendeEnhetService.hentBehandlendeEnhet(krav.identitetsnummer, krav.id.toString())
        requireNotNull(aktoerId) { "Fant ikke AktørID for fnr i ${krav.id}" }
        logger.info("Fant aktørid")
        val beskrivelse = om.writeValueAsString(krav.toKravForOppgave())
        val oppgaveType = "ROB_BEH"
        val request = OpprettOppgaveRequest(
            tildeltEnhetsnr = enhetsNr,
            aktoerId = aktoerId,
            journalpostId = krav.journalpostId,
            beskrivelse = beskrivelse,
            tema = "SYK",
            behandlingstype = digitalKravBehandingsType,
            oppgavetype = oppgaveType,
            behandlingstema = fritakAGPBehandingsTema,
            aktivDato = krav.opprettet.toLocalDate(),
            fristFerdigstillelse = krav.opprettet.toLocalDate().plusDays(7),
            prioritet = "NORM"
        )

        return runBlocking { oppgaveKlient.opprettOppgave(request, UUID.randomUUID().toString()).id.toString() }
    }

    data class JobbData(val id: UUID)
}
