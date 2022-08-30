package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.helse.GravidTestData
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class NaisHealthEndpointTests : SystemTestBase() {

    @Test
    fun `nais isalive endpoint with no JWT returns ProbeResult OK`() = suspendableTest {
        val response = httpClient.get {
            appUrl("/health/is-alive")
            contentType(ContentType.Application.Json)
            setBody(GravidTestData.fullValidSoeknadRequest)
        }
            .body<String>()

        Assertions.assertThat(response).isNotBlank()
    }

    @Test
    fun `nais isready endpoint with no JWT returns 200 OK`() = suspendableTest {
        val response = httpClient.get {
            appUrl("/health/is-ready")
            contentType(ContentType.Application.Json)
            setBody(GravidTestData.fullValidSoeknadRequest)
        }
            .body<String>()

        Assertions.assertThat(response).isNotBlank()
    }
}
