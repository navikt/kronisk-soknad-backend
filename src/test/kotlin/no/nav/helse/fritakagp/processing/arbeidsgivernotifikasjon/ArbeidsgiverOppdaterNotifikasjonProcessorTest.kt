package no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.GravidTestData
import no.nav.helse.KroniskTestData
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.Bakgrunnsjobb
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.enums.SaksStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ArbeidsgiverOppdaterNotifikasjonProcessorTest {

    private fun getResourceAsText(filename: String) =
        this::class.java.classLoader.getResource("responses/$filename")!!.readText()

    val response = getResourceAsText("opprettNySak/gyldig.json")
    val arbeidsgiverNotifikasjonKlient = mockClientArbeidsgiverNotifikasjonKlient()
    val gravidKravRepositoryMock = mockk<GravidKravRepository>(relaxed = true)
    val kroniskKravRepositoryMock = mockk<KroniskKravRepository>(relaxed = true)
    val objectMapper = ObjectMapper().registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    ).registerModule(JavaTimeModule())

    lateinit var gravidKrav: GravidKrav
    lateinit var kroniskKrav: KroniskKrav

    private var gravidJobb = Bakgrunnsjobb(data = "", type = "arbeidsgiveroppdaternotifikasjon")
    private var kroniskJobb = Bakgrunnsjobb(data = "", type = "arbeidsgiveroppdaternotifikasjon")

    val prosessor = ArbeidsgiverOppdaterNotifikasjonProcessor(
        objectMapper,
        arbeidsgiverNotifikasjonKlient
    )

    @BeforeEach
    fun setup() {
        gravidKrav = GravidTestData.gravidKrav.copy()
        kroniskKrav = KroniskTestData.kroniskKrav.copy()
        gravidJobb = Bakgrunnsjobb(data = objectMapper.writeValueAsString(ArbeidsgiverNotifikasjonProcessor.JobbData(gravidKrav.id, ArbeidsgiverNotifikasjonProcessor.JobbData.SkjemaType.GravidKrav)), type = "arbeidsgiveroppdaternotifikasjon")
        kroniskJobb = Bakgrunnsjobb(data = objectMapper.writeValueAsString(ArbeidsgiverNotifikasjonProcessor.JobbData(kroniskKrav.id, ArbeidsgiverNotifikasjonProcessor.JobbData.SkjemaType.KroniskKrav)), type = "arbeidsgiveroppdaternotifikasjon")
        every { gravidKravRepositoryMock.getById(gravidKrav.id) } returns gravidKrav
        every { kroniskKravRepositoryMock.getById(kroniskKrav.id) } returns kroniskKrav
    }

    @Test
    fun `Oppdaterer sak mot arbeidsgiver-notifikasjoner for gravidKrav`() {
        prosessor.prosesser(gravidJobb)
        coVerify(exactly = 1) { arbeidsgiverNotifikasjonKlient.nyStatusSakByGrupperingsid(gravidKrav.id.toString(), any(), SaksStatus.MOTTATT) }
    }

    @Test
    fun `Oppdaterer sak mot arbeidsgiver-notifikasjoner for kroniskKrav`() {
        prosessor.prosesser(kroniskJobb)
        coVerify(exactly = 1) { arbeidsgiverNotifikasjonKlient.nyStatusSakByGrupperingsid(kroniskKrav.id.toString(), any(), SaksStatus.MOTTATT) }
    }
}

fun mockClientArbeidsgiverNotifikasjonKlient(): ArbeidsgiverNotifikasjonKlient {
    val klient = mockk<ArbeidsgiverNotifikasjonKlient>(relaxed = true)
    coEvery { klient.nyStatusSakByGrupperingsid(any(), any(), any()) } returns "313"
    return klient
}
