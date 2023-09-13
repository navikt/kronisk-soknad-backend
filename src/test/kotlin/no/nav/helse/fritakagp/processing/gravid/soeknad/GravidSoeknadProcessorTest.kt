package no.nav.helse.fritakagp.processing.gravid.soeknad

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.helse.GravidTestData
import no.nav.helse.GravidTestData.gravidOpprettOppgaveResponse
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostRequest
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OPPGAVETYPE_FORDELINGSOPPGAVE
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson.PdlFullPersonliste
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson.PdlGeografiskTilknytning.PdlGtType.UTLAND
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson.PdlIdentResponse
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentPersonNavn
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlIdent
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlPersonNavnMetadata
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.integration.gcp.BucketDocument
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor.Jobbdata.SkjemaType
import no.nav.helse.fritakagp.service.BehandlendeEnhetService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.util.Base64

class GravidSoeknadProcessorTest {

    val joarkMock = mockk<DokarkivKlient>(relaxed = true)
    val oppgaveMock = mockk<OppgaveKlient>(relaxed = true)
    val repositoryMock = mockk<GravidSoeknadRepository>(relaxed = true)
    val pdlClientMock = mockk<PdlClient>(relaxed = true)
    val objectMapper = ObjectMapper().registerModule(KotlinModule())
    val pdfGeneratorMock = mockk<GravidSoeknadPDFGenerator>(relaxed = true)
    val bucketStorageMock = mockk<BucketStorage>(relaxed = true)
    val bakgrunnsjobbRepomock = mockk<BakgrunnsjobbRepository>(relaxed = true)
    val berregServiceMock = mockk<BrregClient>(relaxed = true)
    val behandlendeEnhetService = mockk<BehandlendeEnhetService>(relaxed = true)
    val prosessor = GravidSoeknadProcessor(
        repositoryMock,
        joarkMock,
        oppgaveMock,
        pdlClientMock,
        bakgrunnsjobbRepomock,
        pdfGeneratorMock,
        objectMapper,
        bucketStorageMock,
        berregServiceMock,
        behandlendeEnhetService
    )

    lateinit var soeknad: GravidSoeknad

    private val oppgaveId = 9999
    private val arkivReferanse = "12345"
    private var jobb = Bakgrunnsjobb(data = "", type = "test")

    @BeforeEach
    fun setup() {
        soeknad = GravidTestData.soeknadGravid.copy()
        jobb = Bakgrunnsjobb(
            data = objectMapper.writeValueAsString(GravidSoeknadProcessor.JobbData(soeknad.id)),
            type = "test"
        )
        objectMapper.registerModule(JavaTimeModule())
        every { repositoryMock.getById(soeknad.id) } returns soeknad
        every { bucketStorageMock.getDocAsString(any()) } returns null
        every { pdlClientMock.personNavn(soeknad.sendtAv) } returns PdlHentPersonNavn.PdlPersonNavneliste(
            listOf(
                PdlHentPersonNavn.PdlPersonNavneliste.PdlPersonNavn(
                    "Ola",
                    "M",
                    "Avsender",
                    PdlPersonNavnMetadata("freg")
                )
            )
        )
        every { pdlClientMock.fullPerson(soeknad.identitetsnummer) } returns PdlHentFullPerson(
            PdlFullPersonliste(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            PdlIdentResponse(listOf(PdlIdent("aktør-id", PdlIdent.PdlIdentGruppe.AKTORID))),
            PdlHentFullPerson.PdlGeografiskTilknytning(UTLAND, null, null, "SWE")
        )
        every { joarkMock.journalførDokument(any(), any(), any()) } returns JournalpostResponse(
            arkivReferanse,
            true,
            "M",
            null,
            emptyList()
        )
        coEvery { oppgaveMock.opprettOppgave(any(), any()) } returns gravidOpprettOppgaveResponse.copy(id = oppgaveId)
        coEvery { berregServiceMock.getVirksomhetsNavn(soeknad.virksomhetsnummer) } returns "Stark Industries"
    }

    @Test
    fun `skal ikke journalføre når det allerede foreligger en journalpostId, men skal forsøke sletting fra bucket `() {
        soeknad.journalpostId = "joark"
        prosessor.prosesser(jobb)

        verify(exactly = 0) { joarkMock.journalførDokument(any(), any(), any()) }
        verify(exactly = 1) { bucketStorageMock.deleteDoc(soeknad.id) }
    }

    @Test
    fun `Om det finnes ekstra dokumentasjon skal den journalføres og så slettes`() {
        val dokumentData = "test"
        val filtypeArkiv = "pdf"
        val filtypeOrginal = "JSON"
        every { bucketStorageMock.getDocAsString(soeknad.id) } returns BucketDocument(dokumentData, filtypeArkiv)

        val joarkRequest = slot<JournalpostRequest>()
        every { joarkMock.journalførDokument(capture(joarkRequest), any(), any()) } returns JournalpostResponse(
            arkivReferanse,
            true,
            "M",
            null,
            emptyList()
        )

        Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(soeknad))
        prosessor.prosesser(jobb)

        verify(exactly = 1) { bucketStorageMock.getDocAsString(soeknad.id) }
        verify(exactly = 1) { bucketStorageMock.deleteDoc(soeknad.id) }

        assertThat((joarkRequest.captured.dokumenter)).hasSize(2)
        val dokumentasjon =
            joarkRequest.captured.dokumenter.filter { it.brevkode == GravidSoeknadProcessor.dokumentasjonBrevkode }
                .first()

        assertThat(dokumentasjon.dokumentVarianter[0].fysiskDokument).isEqualTo(dokumentData)
        assertThat(dokumentasjon.dokumentVarianter[0].filtype).isEqualTo(filtypeArkiv.uppercase())
        assertThat(dokumentasjon.dokumentVarianter[0].variantFormat).isEqualTo("ARKIV")
        assertThat(dokumentasjon.dokumentVarianter[1].filtype).isEqualTo(filtypeOrginal)
        assertThat(dokumentasjon.dokumentVarianter[1].variantFormat).isEqualTo("ORIGINAL")
    }

    @Test
    fun `skal ikke lage oppgave når det allerede foreligger en oppgaveId `() {
        soeknad.oppgaveId = "ppggssv"
        prosessor.prosesser(jobb)
        coVerify(exactly = 0) { oppgaveMock.opprettOppgave(any(), any()) }
    }

    @Test
    fun `skal opprette fordelingsoppgave når stoppet`() {
        prosessor.stoppet(jobb)

        val oppgaveRequest = CapturingSlot<OpprettOppgaveRequest>()

        coVerify(exactly = 1) { oppgaveMock.opprettOppgave(capture(oppgaveRequest), any()) }
        assertThat(oppgaveRequest.captured.oppgavetype).isEqualTo(OPPGAVETYPE_FORDELINGSOPPGAVE)
    }

    @Test
    fun `skal journalføre, opprette oppgave og oppdatere søknaden i databasen`() {
        prosessor.prosesser(jobb)

        assertThat(soeknad.journalpostId).isEqualTo(arkivReferanse)
        assertThat(soeknad.oppgaveId).isEqualTo(oppgaveId.toString())
        assertThat(soeknad.virksomhetsnavn).isEqualTo("Stark Industries")

        verify(exactly = 1) { joarkMock.journalførDokument(any(), true, any()) }
        coVerify(exactly = 1) { oppgaveMock.opprettOppgave(any(), any()) }
        verify(exactly = 1) { repositoryMock.update(soeknad) }
        coVerify(exactly = 1) { berregServiceMock.getVirksomhetsNavn(soeknad.virksomhetsnummer) }
    }

    @Test
    fun `skal opprette jobber`() {
        prosessor.prosesser(jobb)

        val opprettetJobber = mutableListOf<Bakgrunnsjobb>()

        verify(exactly = 2) {
            bakgrunnsjobbRepomock.save(capture(opprettetJobber))
        }

        val kafkajobb = opprettetJobber.find { it.type == GravidSoeknadKafkaProcessor.JOB_TYPE }
        assertThat(kafkajobb?.data).contains(soeknad.id.toString())

        val beskjedJobb = opprettetJobber.find { it.type == BrukernotifikasjonProcessor.JOB_TYPE }
        assertThat(beskjedJobb?.data).contains(soeknad.id.toString())
        assertThat(beskjedJobb?.data).contains(SkjemaType.GravidSøknad.name)
    }

    @Test
    fun `Ved feil i oppgave skal joarkref lagres, og det skal det kastes exception oppover`() {
        coEvery { oppgaveMock.opprettOppgave(any(), any()) } throws IOException()

        assertThrows<IOException> { prosessor.prosesser(jobb) }

        assertThat(soeknad.journalpostId).isEqualTo(arkivReferanse)
        assertThat(soeknad.oppgaveId).isNull()

        verify(exactly = 1) { joarkMock.journalførDokument(any(), true, any()) }
        coVerify(exactly = 1) { oppgaveMock.opprettOppgave(any(), any()) }
        verify(exactly = 1) { repositoryMock.update(soeknad) }
    }
}
