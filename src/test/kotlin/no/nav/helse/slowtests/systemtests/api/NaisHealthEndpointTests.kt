package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.GravidTestData
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class NaisHealthEndpointTests : SystemTestBase() {

    @Test
    fun `nais isalive endpoint with no JWT returns ProbeResult OK`() = suspendableTest {
        val response = httpClient.get<String> {
            appUrl("/health/is-alive")
            contentType(ContentType.Application.Json)
            body = GravidTestData.fullValidRequest
        }

        Assertions.assertThat(response).isNotBlank()
    }

    @Test
    fun `nais isready endpoint with no JWT returns 200 OK`() = suspendableTest {
        val response = httpClient.get<String> {
            appUrl("/health/is-ready")
            contentType(ContentType.Application.Json)
            body = GravidTestData.fullValidRequest
        }

        Assertions.assertThat(response).isNotBlank()
    }
}
