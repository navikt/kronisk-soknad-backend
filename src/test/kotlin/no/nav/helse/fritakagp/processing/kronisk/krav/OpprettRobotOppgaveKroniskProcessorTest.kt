package no.nav.helse.fritakagp.processing.kronisk.krav

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.KroniskTestData
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentPersonNavn
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlIdent
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlPersonNavnMetadata
import no.nav.helse.fritakagp.customObjectMapper
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.processing.BakgrunnsJobbUtils
import no.nav.helse.fritakagp.service.BehandlendeEnhetService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OpprettRobotOppgaveKroniskProcessorTest {
    val oppgaveMock = mockk<OppgaveKlient>(relaxed = true)
    val repositoryMock = mockk<KroniskKravRepository>(relaxed = true)
    val pdlClientMock = mockk<PdlClient>(relaxed = true)
    val objectMapper = customObjectMapper()
    val behandlendeEnhetService = mockk<BehandlendeEnhetService>(relaxed = true)
    val prosessor = OpprettRobotOppgaveKroniskProcessor(repositoryMock, oppgaveMock, pdlClientMock, objectMapper, behandlendeEnhetService)
    lateinit var krav: KroniskKrav

    private val oppgaveId = 9999
    private var jobb = BakgrunnsJobbUtils.emptyJob()

    @BeforeEach
    fun setup() {
        krav = KroniskTestData.kroniskKrav.copy()
        jobb = BakgrunnsJobbUtils.testJob(objectMapper.writeValueAsString(OpprettRobotOppgaveKroniskProcessor.JobbData(krav.id)))
        every { repositoryMock.getById(krav.id) } returns krav
        every { pdlClientMock.personNavn(krav.sendtAv) } returns PdlHentPersonNavn.PdlPersonNavneliste(
            listOf(
                PdlHentPersonNavn.PdlPersonNavneliste.PdlPersonNavn("Ola", "M", "Avsender", PdlPersonNavnMetadata("freg"))
            )
        )
        every { pdlClientMock.fullPerson(krav.identitetsnummer) } returns PdlHentFullPerson(
            PdlHentFullPerson.PdlFullPersonliste(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            PdlHentFullPerson.PdlIdentResponse(listOf(PdlIdent("aktør-id", PdlIdent.PdlIdentGruppe.AKTORID))),
            PdlHentFullPerson.PdlGeografiskTilknytning(PdlHentFullPerson.PdlGeografiskTilknytning.PdlGtType.UTLAND, null, null, "SWE")
        )
        coEvery { oppgaveMock.opprettOppgave(any(), any()) } returns KroniskTestData.kroniskOpprettOppgaveResponse.copy(id = oppgaveId)
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
