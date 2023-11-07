package no.nav.helse.fritakagp.processing.gravid.krav

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.GravidTestData
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.OppgaveKlient
import no.nav.helse.fritakagp.customObjectMapper
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.processing.BakgrunnsJobbUtils
import no.nav.helse.fritakagp.service.BehandlendeEnhetService
import no.nav.helse.fritakagp.service.PdlService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OpprettRobotOppgaveGravidProcessorTest {
    val oppgaveMock = mockk<OppgaveKlient>(relaxed = true)
    val repositoryMock = mockk<GravidKravRepository>(relaxed = true)
    val pdlServiceMock = mockk<PdlService>(relaxed = true)
    val objectMapper = customObjectMapper()
    val behandlendeEnhetService = mockk<BehandlendeEnhetService>(relaxed = true)
    val prosessor = OpprettRobotOppgaveGravidProcessor(repositoryMock, oppgaveMock, pdlServiceMock, objectMapper, behandlendeEnhetService)
    lateinit var krav: GravidKrav

    private val oppgaveId = 9999
    private var jobb = BakgrunnsJobbUtils.emptyJob()

    @BeforeEach
    fun setup() {
        krav = GravidTestData.gravidKrav.copy()
        jobb = BakgrunnsJobbUtils.testJob(objectMapper.writeValueAsString(OpprettRobotOppgaveGravidProcessor.JobbData(krav.id)))
        every { repositoryMock.getById(krav.id) } returns krav
        every { pdlServiceMock.hentNavn(krav.sendtAv) } returns "aktør-id"
        coEvery { oppgaveMock.opprettOppgave(any(), any()) } returns GravidTestData.gravidOpprettOppgaveResponse.copy(id = oppgaveId)
    }

    @Test
    fun `skal journalføre, opprette oppgave og oppdatere søknaden i databasen`() {
        prosessor.prosesser(jobb)

        Assertions.assertThat(krav.oppgaveId).isEqualTo(oppgaveId.toString())

        coVerify(exactly = 1) {
            oppgaveMock.opprettOppgave(
                withArg {
                    assertEquals("ROB_BEH", it.oppgavetype)
                },
                any()
            )
        }
        verify(exactly = 1) { repositoryMock.update(krav) }
    }

    @Test
    fun `skal ikke lage oppgave når det allerede foreligger en oppgaveId `() {
        krav.oppgaveId = "ppggssv"
        prosessor.prosesser(jobb)
        coVerify(exactly = 0) { oppgaveMock.opprettOppgave(any(), any()) }
    }
}
