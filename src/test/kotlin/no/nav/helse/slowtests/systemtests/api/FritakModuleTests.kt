package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.GravidTestData
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class FritakModuleTests : SystemTestBase() {
    private val soeknadGravidUrl = "/api/v1/gravid/soeknad"

    @Test
    fun `invalid enum fields gives 400 Bad request`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(soeknadGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")

            body = """
                {
                    "fnr": "${GravidTestData.validIdentitetsnummer}",
                    "orgnr": "${GravidTestData.fullValidRequest.orgnr}",
                    "tilrettelegge": true,
                    "tiltak": ["IKKE GYLDIG"]
                }
            """.trimIndent()
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    }

    @Test
    fun `Skal returnere Created ved feilfritt skjema uten fil`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(soeknadGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.fullValidRequest
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.Created)
    }

    @Test
    fun `Skal returnere Created når fil er vedlagt`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(soeknadGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.gravidSoknadMedFil
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.Created)
    }
}