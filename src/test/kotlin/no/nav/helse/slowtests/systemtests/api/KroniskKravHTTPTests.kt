package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.KroniskTestData
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class KroniskKravHTTPTests : SystemTestBase() {
    private val kravKroniskUrl = "/api/v1/kronisk/krav"

    @Test
    fun `invalid json gives 400 Bad request`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravKroniskUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")

            body = """
                {
                    "fnr": "${KroniskTestData.validIdentitetsnummer}",
                    "orgnr": "${KroniskTestData.fullValidRequest.virksomhetsnummer}",
                    "tilrettelegge": fele,
                    "tiltak": ["IKKE GYLDIG"]
                }
            """.trimIndent()
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
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
        val response = httpClient.post<HttpResponse> {
            appUrl(kravKroniskUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = KroniskTestData.kroniskKravRequestValid.copy(virksomhetsnummer = "123456785")
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
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