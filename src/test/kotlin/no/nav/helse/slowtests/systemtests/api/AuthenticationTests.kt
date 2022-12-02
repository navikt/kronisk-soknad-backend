package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.GravidTestData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AuthenticationTests : SystemTestBase() {
    private val soeknadGravidUrl = "/api/v1/gravid/soeknad"

    @Test
    fun `posting application with no JWT returns 401 Unauthorized`() = suspendableTest {
        val exception = assertThrows<ClientRequestException>
        {
            httpClient.post<HttpResponse> {
                appUrl(soeknadGravidUrl)
                contentType(ContentType.Application.Json)
                body = GravidTestData.fullValidSoeknadRequest
            }
        }

        assertThat(exception.response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `posting application with Valid JWT does not return 401 Unauthorized`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(soeknadGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")

            body = GravidTestData.fullValidSoeknadRequest
        }

        assertThat(response.status).isNotEqualTo(HttpStatusCode.Unauthorized)
    }
}
