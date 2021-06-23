package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.GravidTestData
import no.nav.helse.KroniskTestData
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.web.api.resreq.PostListResponseDto
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.test.inject
import java.time.LocalDate

class KroniskKravHTTPTests : SystemTestBase() {
    private val kravKroniskUrl = "/api/v1/kronisk/krav"

    @Test
    internal fun `Returnerer kravet når korrekt bruker er innlogget, 404 når ikke`() = suspendableTest {
        val repo by inject<KroniskKravRepository>()
        repo.insert(KroniskTestData.kroniskKrav)

        val notFoundException = assertThrows<ClientRequestException>
        {
            httpClient.get<HttpResponse> {
                appUrl("$kravKroniskUrl/${KroniskTestData.kroniskKrav.id}")
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
            }
        }
        Assertions.assertThat(notFoundException.response.status).isEqualTo(HttpStatusCode.NotFound)

        val accessGrantedForm = httpClient.get<KroniskKrav> {
            appUrl("$kravKroniskUrl/${KroniskTestData.kroniskKrav.id}")
            contentType(ContentType.Application.Json)
            loggedInAs(KroniskTestData.kroniskKrav.identitetsnummer)
        }

        Assertions.assertThat(accessGrantedForm).isEqualTo(KroniskTestData.kroniskKrav)
    }

    @Test
    fun `invalid json gives 400 Bad request`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravKroniskUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")

            body = """
                {
                    "identitetsnummer": "${KroniskTestData.validIdentitetsnummer}",
                    "virksomhetsnummer": "${KroniskTestData.fullValidRequest.virksomhetsnummer}",
                    "perioder": fele,
                    "bekreftet": ["IKKE GYLDIG"]
                    "dokumentasjon": ["IKKE GYLDIG"]
                    "kontrollDager": ["IKKE GYLDIG"]
                }
            """.trimIndent()
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val res = extractResponseBody(response)
        Assertions.assertThat(res.genericMessage).contains("Cannot construct instance of")
    }

    @Test
    fun `Skal returnere Created ved feilfritt skjema uten fil`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravKroniskUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = KroniskTestData.kroniskKravRequestValid
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `Skal returnere forbidden hvis virksomheten ikke er i auth listen fra altinn`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravKroniskUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = KroniskTestData.kroniskKravRequestValid.copy(virksomhetsnummer = "123456785")
        }
        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val res = extractResponseBody(response)
        Assertions.assertThat(res.status).isEqualTo(PostListResponseDto.Status.GENERIC_ERROR)
    }

    @Test
    fun `Skal returnere Created når fil er vedlagt`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravKroniskUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = KroniskTestData.kroniskKravRequestMedFil
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }


    @Test
    fun `Skal returnere full propertypath for periode`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravKroniskUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = KroniskTestData.kroniskKravRequestInValid.copy(perioder = setOf(
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
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2020, 1, 5),
                    LocalDate.of(2020, 1, 14),
                    12,
                    månedsinntekt = 2590.8,
                )
            ))
        }
        val possiblePropertyPaths = setOf(
            "perioder[0].fom",
            "perioder[0].antallDagerMedRefusjon",
            "perioder[1].antallDagerMedRefusjon",
            "perioder[2].fom",
            "perioder[2].antallDagerMedRefusjon",
        )
        val res = extractResponseBody(response)
        Assertions.assertThat(res.status).isEqualTo(PostListResponseDto.Status.VALIDATION_ERRORS)
        Assertions.assertThat(res.validationErrors.size).isEqualTo(5)
        Assertions.assertThat(res.validationErrors[0].propertyPath).isIn(possiblePropertyPaths)
        Assertions.assertThat(res.validationErrors[1].propertyPath).isIn(possiblePropertyPaths)
        Assertions.assertThat(res.validationErrors[2].propertyPath).isIn(possiblePropertyPaths)
        Assertions.assertThat(res.validationErrors[3].propertyPath).isIn(possiblePropertyPaths)
        Assertions.assertThat(res.validationErrors[4].propertyPath).isIn(possiblePropertyPaths)
    }
}