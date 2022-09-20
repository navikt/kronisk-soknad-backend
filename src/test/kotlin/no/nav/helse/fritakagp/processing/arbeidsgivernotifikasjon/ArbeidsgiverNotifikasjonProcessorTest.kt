package no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.helse.GravidTestData
import no.nav.helse.KroniskTestData
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.opprettNySak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ArbeidsgiverNotifikasjonProcessorTest {
    val gravidKravRepositoryMock = mockk<GravidKravRepository>(relaxed = true)
    val kroniskKravRepositoryMock = mockk<KroniskKravRepository>(relaxed = true)
    val arbeidsgiverNotifikasjonKlient = mockk<ArbeidsgiverNotifikasjonKlient>()
    val objectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    lateinit var gravidKrav: GravidKrav
    lateinit var kroniskKrav: KroniskKrav

    private var gravidJobb = Bakgrunnsjobb(data = "", type = "test")
    private var kroniskJobb = Bakgrunnsjobb(data = "", type = "test")

    val prosessor = ArbeidsgiverNotifikasjonProcessor(
        gravidKravRepositoryMock,
        kroniskKravRepositoryMock,
        objectMapper,
        "http://localhost:8080/",
        arbeidsgiverNotifikasjonKlient
    )

    @BeforeEach
    fun setup() {
        gravidKrav = GravidTestData.gravidKrav.copy()
        kroniskKrav = KroniskTestData.kroniskKrav.copy()
        gravidJobb = Bakgrunnsjobb(data = objectMapper.writeValueAsString(ArbeidsgiverNotifikasjonProcessor.JobbData(gravidKrav.id, ArbeidsgiverNotifikasjonProcessor.JobbData.SkjemaType.GravidKrav)), type = "test")
        kroniskJobb = Bakgrunnsjobb(data = objectMapper.writeValueAsString(ArbeidsgiverNotifikasjonProcessor.JobbData(kroniskKrav.id, ArbeidsgiverNotifikasjonProcessor.JobbData.SkjemaType.KroniskKrav)), type = "test")

        every { gravidKravRepositoryMock.getById(gravidKrav.id) } returns gravidKrav
        every { kroniskKravRepositoryMock.getById(kroniskKrav.id) } returns kroniskKrav

        mockkStatic(arbeidsgiverNotifikasjonKlient::opprettNySak)
        coEvery { arbeidsgiverNotifikasjonKlient.opprettNySak(any(), any(), any(), any(), any(), any()) } returns "1"
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(arbeidsgiverNotifikasjonKlient::opprettNySak)
    }

    @Test
    fun `Oppretter ny sak mot arbeidsgiver-notifikasjoner for gravidKrav`() {
        prosessor.prosesser(gravidJobb)
        val krav = gravidKravRepositoryMock.getById(gravidKrav.id)
        assertThat(krav?.arbeidsgiverSakId).isEqualTo("1")
    }

    @Test
    fun `Oppretter ny sak mot arbeidsgiver-notifikasjoner for kroniskKrav`() {
        prosessor.prosesser(kroniskJobb)
        val krav = kroniskKravRepositoryMock.getById(kroniskKrav.id)
        assertThat(krav?.arbeidsgiverSakId).isEqualTo("1")
    }
}
