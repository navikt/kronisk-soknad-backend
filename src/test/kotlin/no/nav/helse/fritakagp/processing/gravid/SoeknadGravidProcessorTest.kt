package no.nav.helse.fritakagp.processing.gravid

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.*
import no.nav.helse.TestData
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveResponse
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.*
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson.PdlFullPersonliste
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson.PdlFullPersonliste.PdlGeografiskTilknytning
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson.PdlFullPersonliste.PdlGeografiskTilknytning.PdlGtType.UTLAND
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson.PdlIdentResponse
import no.nav.helse.fritakagp.db.PostgresGravidSoeknadRepository
import no.nav.helse.fritakagp.domain.SoeknadGravid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

class SoeknadGravidProcessorTest {

    val joarkMock = mockk<DokarkivKlient>(relaxed = true)
    val oppgaveMock = mockk<OppgaveKlient>(relaxed = true)
    val repositoryMock = mockk<PostgresGravidSoeknadRepository>(relaxed = true)
    val pdlClientMock = mockk<PdlClient>(relaxed = true)
    val objectMapper = ObjectMapper().registerModule(KotlinModule())
    val pdfGeneratorMock = mockk<GravidSoeknadPDFGenerator>(relaxed = true)
    val refusjonskravBehandler = SoeknadGravidProcessor(repositoryMock, joarkMock, oppgaveMock, pdlClientMock, pdfGeneratorMock, objectMapper)
    lateinit var soeknad: SoeknadGravid

    private val oppgaveId = 9999
    private val arkivReferanse = "12345"
    private var jobbDataJson = ""

    @BeforeEach
    fun setup() {
        soeknad = TestData.soeknadGravid.copy()
        jobbDataJson = objectMapper.writeValueAsString(SoeknadGravidProcessor.JobbData(soeknad.id))
        every { repositoryMock.getById(soeknad.id) } returns soeknad
        every { pdlClientMock.personNavn(soeknad.sendtAv)} returns PdlHentPersonNavn.PdlPersonNavneliste(listOf(
            PdlHentPersonNavn.PdlPersonNavneliste.PdlPersonNavn("Ola", "M", "Avsender", PdlPersonNavnMetadata("freg"))))
        every { pdlClientMock.fullPerson(soeknad.fnr)} returns PdlHentFullPerson(
            PdlFullPersonliste(emptyList(), emptyList(), PdlGeografiskTilknytning(UTLAND, null, null, "SWE"), emptyList(), emptyList(), emptyList()),
            PdlIdentResponse(listOf(PdlIdent("aktør-id", PdlIdent.PdlIdentGruppe.AKTORID)))
        )
        every { joarkMock.journalførDokument(any(), any(), any()) } returns JournalpostResponse(arkivReferanse, true, "M", null, emptyList())
        coEvery { oppgaveMock.opprettOppgave(any(), any())} returns OpprettOppgaveResponse(oppgaveId)
    }


    @Test
    fun `skal ikke journalføre når det allerede foreligger en journalpostId `() {
        soeknad.journalpostId = "joark"
        refusjonskravBehandler.prosesser(jobbDataJson)

        verify(exactly = 0) { joarkMock.journalførDokument(any(), any(), any()) }
    }

    @Test
    fun `skal ikke lage oppgave når det allerede foreligger en oppgaveId `() {
        soeknad.oppgaveId = "ppggssv"
        refusjonskravBehandler.prosesser(jobbDataJson)
        coVerify(exactly = 0) { oppgaveMock.opprettOppgave(any(), any()) }
    }

    @Test
    fun `skal journalføre, opprette oppgave og oppdatere søknaden i databasen`() {
        refusjonskravBehandler.prosesser(jobbDataJson)

        assertThat(soeknad.journalpostId).isEqualTo(arkivReferanse)
        assertThat(soeknad.oppgaveId).isEqualTo(oppgaveId.toString())

        verify(exactly = 1) { joarkMock.journalførDokument(any(), true, any()) }
        coVerify(exactly = 1) { oppgaveMock.opprettOppgave(any(), any()) }
        verify(exactly = 1) { repositoryMock.update(soeknad) }
    }


    @Test
    fun `Ved feil i oppgave skal joarkref lagres, og det skal det kastes exception oppover`() {

        coEvery { oppgaveMock.opprettOppgave(any(), any()) } throws IOException()

        assertThrows<IOException> { refusjonskravBehandler.prosesser(jobbDataJson) }

        assertThat(soeknad.journalpostId).isEqualTo(arkivReferanse)
        assertThat(soeknad.oppgaveId).isNull()

        verify(exactly = 1) { joarkMock.journalførDokument(any(), true, any()) }
        coVerify(exactly = 1) { oppgaveMock.opprettOppgave(any(), any()) }
        verify(exactly = 1) { repositoryMock.update(soeknad) }
    }

}