package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
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
