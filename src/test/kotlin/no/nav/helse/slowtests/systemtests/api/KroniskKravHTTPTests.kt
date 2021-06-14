package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.KroniskTestData
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.domain.KroniskKrav
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.test.inject

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
        val exception = assertThrows<ClientRequestException>
        {
            httpClient.post<HttpResponse> {
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
        }

        Assertions.assertThat(exception.response.status).isEqualTo(HttpStatusCode.BadRequest)
        Assertions.assertThat(exception.message).contains("400 Bad Request")
    }

    @Test
    fun `Skal returnere Created ved feilfritt skjema uten fil`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravKroniskUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = KroniskTestData.kroniskKravRequestValid
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.Created)
    }

    @Test
    fun `Skal returnere forbidden hvis virksomheten ikke er i auth listen fra altinn`() = suspendableTest {
        val exception = assertThrows<ClientRequestException>
        {
            httpClient.post<HttpResponse> {
                appUrl(kravKroniskUrl)
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
                body = KroniskTestData.kroniskKravRequestValid.copy(virksomhetsnummer = "123456785")
            }
        }

        Assertions.assertThat(exception.response.status).isEqualTo(HttpStatusCode.Forbidden)
    }

    @Test
    fun `Skal returnere Created når fil er vedlagt`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravKroniskUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = KroniskTestData.kroniskKravRequestMedFil
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.Created)
    }
}