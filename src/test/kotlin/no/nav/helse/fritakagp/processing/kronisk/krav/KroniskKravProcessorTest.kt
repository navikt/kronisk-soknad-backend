package no.nav.helse.fritakagp.processing.kronisk.krav

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.*
import no.nav.helse.KroniskTestData
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostRequest
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OPPGAVETYPE_FORDELINGSOPPGAVE
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveResponse
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.*
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson.PdlFullPersonliste
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson.PdlGeografiskTilknytning.PdlGtType.UTLAND
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson.PdlIdentResponse
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.integration.brreg.BerregClient
import no.nav.helse.fritakagp.integration.gcp.BucketDocument
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.processing.BakgrunnsJobbUtils.emptyJob
import no.nav.helse.fritakagp.processing.BakgrunnsJobbUtils.testJob
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

class KroniskKravProcessorTest {

    val joarkMock = mockk<DokarkivKlient>(relaxed = true)
    val oppgaveMock = mockk<OppgaveKlient>(relaxed = true)
    val repositoryMock = mockk<KroniskKravRepository>(relaxed = true)
    val pdlClientMock = mockk<PdlClient>(relaxed = true)
    val objectMapper = ObjectMapper().registerModule(KotlinModule())
    val pdfGeneratorMock = mockk<KroniskKravPDFGenerator>(relaxed = true)
    val bucketStorageMock = mockk<BucketStorage>(relaxed = true)
    val bakgrunnsjobbRepomock = mockk<BakgrunnsjobbRepository>(relaxed = true)
    val berregClient = mockk<BerregClient>(relaxed = true)
    val prosessor = KroniskKravProcessor(repositoryMock, joarkMock, oppgaveMock, pdlClientMock, bakgrunnsjobbRepomock, pdfGeneratorMock, objectMapper, bucketStorageMock, berregClient)
    lateinit var krav: KroniskKrav

    private val oppgaveId = 9999
    private val arkivReferanse = "12345"
    private var jobb = emptyJob()

    @BeforeEach
    fun setup() {
        objectMapper.registerModule(JavaTimeModule())
        krav = KroniskTestData.kroniskKrav.copy()
        jobb = testJob(objectMapper.writeValueAsString(KroniskKravProcessor.JobbData(krav.id)))
        every { repositoryMock.getById(krav.id) } returns krav
        every { bucketStorageMock.getDocAsString(any()) } returns null
        every { pdlClientMock.personNavn(krav.sendtAv)} returns PdlHentPersonNavn.PdlPersonNavneliste(listOf(
            PdlHentPersonNavn.PdlPersonNavneliste.PdlPersonNavn("Ola", "M", "Avsender", PdlPersonNavnMetadata("freg"))))
        every { pdlClientMock.fullPerson(krav.identitetsnummer)} returns PdlHentFullPerson(
            PdlFullPersonliste(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            PdlIdentResponse(listOf(PdlIdent("aktør-id", PdlIdent.PdlIdentGruppe.AKTORID))),
            PdlHentFullPerson.PdlGeografiskTilknytning(UTLAND, null, null, "SWE")
        )
        every { joarkMock.journalførDokument(any(), any(), any()) } returns JournalpostResponse(arkivReferanse, true, "M", null, emptyList())
        coEvery { oppgaveMock.opprettOppgave(any(), any())} returns OpprettOppgaveResponse(oppgaveId)
    }


    @Test
    fun `skal ikke journalføre når det allerede foreligger en journalpostId, men skal forsøke sletting fra bucket `() {
        krav.journalpostId = "joark"
        prosessor.prosesser(jobb)

        verify(exactly = 0) { joarkMock.journalførDokument(any(), any(), any()) }
        verify(exactly = 1) { bucketStorageMock.deleteDoc(krav.id) }
    }

    @Test
    fun `skal opprette fordelingsoppgave når stoppet`() {
        prosessor.stoppet(jobb)

        val oppgaveRequest = CapturingSlot<OpprettOppgaveRequest>()

        coVerify(exactly = 1) { oppgaveMock.opprettOppgave(capture(oppgaveRequest), any()) }
        assertThat(oppgaveRequest.captured.oppgavetype).isEqualTo(OPPGAVETYPE_FORDELINGSOPPGAVE)
    }

    @Test
    fun `Om det finnes ekstra dokumentasjon skal den journalføres og så slettes`() {
        val dokumentData = "test"
        val filtypeArkiv = "pdf"
        val filtypeOrginal = "json"

        every { bucketStorageMock.getDocAsString(krav.id) } returns BucketDocument(dokumentData, filtypeArkiv)

        val joarkRequest = slot<JournalpostRequest>()
        every { joarkMock.journalførDokument(capture(joarkRequest), any(), any()) } returns JournalpostResponse(arkivReferanse, true, "M", null, emptyList())

        val orginalJsonDoc = objectMapper.writeValueAsString(krav)
        prosessor.prosesser(jobb)

        verify(exactly = 1) { bucketStorageMock.getDocAsString(krav.id) }
        verify(exactly = 1) { bucketStorageMock.deleteDoc(krav.id) }

        assertThat((joarkRequest.captured.dokumenter)).hasSize(2)
        val dokumentasjon = joarkRequest.captured.dokumenter.filter { it.brevkode == KroniskKravProcessor.dokumentasjonBrevkode }.first()

        assertThat(dokumentasjon.dokumentVarianter[0].fysiskDokument).isEqualTo(dokumentData)
        assertThat(dokumentasjon.dokumentVarianter[0].filtype).isEqualTo(filtypeArkiv.toUpperCase())
        assertThat(dokumentasjon.dokumentVarianter[0].variantFormat).isEqualTo("ARKIV")
        assertThat(dokumentasjon.dokumentVarianter[1].fysiskDokument).isEqualTo(orginalJsonDoc)
        assertThat(dokumentasjon.dokumentVarianter[1].filtype).isEqualTo(filtypeOrginal)
        assertThat(dokumentasjon.dokumentVarianter[1].variantFormat).isEqualTo("ORGINAL")
    }

    @Test
    fun `skal ikke lage oppgave når det allerede foreligger en oppgaveId `() {
        krav.oppgaveId = "ppggssv"
        prosessor.prosesser(jobb)
        coVerify(exactly = 0) { oppgaveMock.opprettOppgave(any(), any()) }
    }

    @Test
    fun `skal journalføre, opprette oppgave og oppdatere søknaden i databasen`() {
        prosessor.prosesser(jobb)

        assertThat(krav.journalpostId).isEqualTo(arkivReferanse)
        assertThat(krav.oppgaveId).isEqualTo(oppgaveId.toString())

        verify(exactly = 1) { joarkMock.journalførDokument(any(), true, any()) }
        coVerify(exactly = 1) { oppgaveMock.opprettOppgave(any(), any()) }
        verify(exactly = 1) { repositoryMock.update(krav) }
    }

    @Test
    fun `skal opprette kafkasenderjobb`() {
        prosessor.prosesser(jobb)

        assertThat(krav.journalpostId).isEqualTo(arkivReferanse)
        assertThat(krav.oppgaveId).isEqualTo(oppgaveId.toString())

        val opprettetBakgrunnsjobb = slot<Bakgrunnsjobb>()
        verify(exactly = 1) { bakgrunnsjobbRepomock.save(capture(opprettetBakgrunnsjobb)) }

        assertThat(opprettetBakgrunnsjobb.captured.type).isEqualTo(KroniskKravKafkaProcessor.JOB_TYPE)
        assertThat(opprettetBakgrunnsjobb.captured.data).contains(krav.id.toString())
    }

    @Test
    fun `Ved feil i oppgave skal joarkref lagres, og det skal det kastes exception oppover`() {

        coEvery { oppgaveMock.opprettOppgave(any(), any()) } throws IOException()

        assertThrows<IOException> { prosessor.prosesser(jobb) }

        assertThat(krav.journalpostId).isEqualTo(arkivReferanse)
        assertThat(krav.oppgaveId).isNull()

        verify(exactly = 1) { joarkMock.journalførDokument(any(), true, any()) }
        coVerify(exactly = 1) { oppgaveMock.opprettOppgave(any(), any()) }
        verify(exactly = 1) { repositoryMock.update(krav) }
    }
}