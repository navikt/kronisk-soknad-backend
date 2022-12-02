package no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.GravidTestData
import no.nav.helse.KroniskTestData
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URL

internal class ArbeidsgiverNotifikasjonProcessorTest {
    private fun getResourceAsText(filename: String) =
        this::class.java.classLoader.getResource("responses/$filename")!!.readText()

    val response = getResourceAsText("opprettNySak/gyldig.json")
    val arbeidsgiverNotifikasjonKlient = buildClientArbeidsgiverNotifikasjonKlient(response)
    val gravidKravRepositoryMock = mockk<GravidKravRepository>(relaxed = true)
    val kroniskKravRepositoryMock = mockk<KroniskKravRepository>(relaxed = true)
    val objectMapper = ObjectMapper().registerModule(KotlinModule()).registerModule(JavaTimeModule())

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

class AccessTokenProviderMock : AccessTokenProvider {
    override fun getToken(): String = "fake token"
}

fun buildClientArbeidsgiverNotifikasjonKlient(
    response: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    headers: Headers = headersOf(HttpHeaders.ContentType, "application/json")
): ArbeidsgiverNotifikasjonKlient {
    val mockEngine = MockEngine {
        respond(
            content = ByteReadChannel(response),
            status = status,
            headers = headers
        )
    }

    return ArbeidsgiverNotifikasjonKlient(
        URL("https://notifikasjon-fake-produsent-api.labs.nais.io/"),
        AccessTokenProviderMock(),
        HttpClient(mockEngine) { install(JsonFeature) }
    )
}
