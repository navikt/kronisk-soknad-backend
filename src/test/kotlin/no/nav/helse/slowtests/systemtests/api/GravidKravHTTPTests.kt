package no.nav.helse.slowtests.systemtests.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.web.api.resreq.PostListResponseDto
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.core.inject
import org.koin.test.inject
import java.time.LocalDate
import kotlin.test.assertFailsWith

class GravidKravHTTPTests : SystemTestBase() {
    private val kravGravidUrl = "/api/v1/gravid/krav"

    @Test
    internal fun `Returnerer kravet når korrekt bruker er innlogget, 404 når ikke`() = suspendableTest {
        val repo by inject<GravidKravRepository>()

        repo.insert(GravidTestData.gravidKrav)
        val exception = assertThrows<ClientRequestException>
        {
            httpClient.get<HttpResponse> {
                appUrl("$kravGravidUrl/${GravidTestData.gravidKrav.id}")
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
            }
        }

        Assertions.assertThat(exception.response.status).isEqualTo(HttpStatusCode.NotFound)

        val accessGrantedForm = httpClient.get<GravidKrav> {
            appUrl("$kravGravidUrl/${GravidTestData.gravidKrav.id}")
            contentType(ContentType.Application.Json)
            loggedInAs(GravidTestData.gravidKrav.identitetsnummer)
        }

        Assertions.assertThat(accessGrantedForm).isEqualTo(GravidTestData.gravidKrav)
    }


    @Test
    fun `invalid json gives 400 Bad request`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")

            body = """
                {
                    "fnr": "${GravidTestData.validIdentitetsnummer}",
                    "orgnr": "${GravidTestData.fullValidSoeknadRequest.virksomhetsnummer}",
                    "tilrettelegge": true,
                    "tiltak": ["IKKE GYLDIG"]
                }
            """.trimIndent()
        }
        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val res = extractResponseBody(response)
        Assertions.assertThat(res.status).isEqualTo(PostListResponseDto.Status.GENERIC_ERROR)
    }

    @Test
    fun `Skal returnere Created ved feilfritt skjema uten fil`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.gravidKravRequestValid
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `Skal returnere Created ved periode på en dag`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.gravidKravRequestValidPeriode1Dag
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `Skal returnere forbidden hvis virksomheten ikke er i auth listen fra altinn`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.gravidKravRequestValid.copy(virksomhetsnummer = "123456785")
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val res = extractResponseBody(response)
        Assertions.assertThat(res.status).isEqualTo(PostListResponseDto.Status.GENERIC_ERROR)
        Assertions.assertThat(res.validationErrors.size).isEqualTo(0)
    }

    @Test
    fun `Skal returnere Created og lagre flagg når fil er vedlagt`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.gravidKravRequestMedFil
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `Skal returnere en valideringfeil`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.gravidKravRequestInValid.copy(perioder = setOf(
                Arbeidsgiverperiode(
                LocalDate.of(2020, 1, 15),
                LocalDate.of(2020, 1, 10),
                2,
                månedsinntekt = 2590.8
            ),
                Arbeidsgiverperiode(
                    LocalDate.of(2020, 1, 5),
                    LocalDate.of(2020, 1, 4),
                    2,
                    månedsinntekt = 2590.8,
                    index = 1
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2020, 1, 5),
                    LocalDate.of(2020, 1, 14),
                    12,
                    månedsinntekt = 2590.8,
                    index = 2
                )
            ))
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val res = extractResponseBody(response)
        Assertions.assertThat(res.status).isEqualTo(PostListResponseDto.Status.VALIDATION_ERRORS)
        Assertions.assertThat(res.validationErrors.size).isEqualTo(3)


    }

}