package no.nav.helse.fritakagp.auth

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.fritakagp.Env
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@WireMockTest(httpPort = 8080)
class AuthClientTest {
    private val env = mockk<Env>(relaxed = true).apply {
        every { tokenEndpoint } returns "http://localhost:8080/token"
    }

    @Test
    fun testTokenReturnSuccess(wmRuntimeInfo: WireMockRuntimeInfo) {
        stubFor(
            post(urlEqualTo("/token")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{"access_token": "token", "expires_in": 3600}""")
            )
        )

        runBlocking {
            val response = AuthClient(env, IdentityProvider.MASKINPORTEN).token("test")
            assertTrue(response is TokenResponse.Success)
            assertEquals("token", (response as TokenResponse.Success).accessToken)
        }
    }

    @Test
    fun testTokenReturnError(wmRuntimeInfo: WireMockRuntimeInfo) {
        stubFor(
            post(urlEqualTo("/token")).willReturn(
                aResponse()
                    .withStatusMessage("Bad Request")
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """{
                              "error": "invalid_request",
                              "error_description": "Invalid request"
                                }"""
                    )
            )
        )

        runBlocking {
            val response = AuthClient(env, IdentityProvider.MASKINPORTEN).token("test")
            assertTrue(response is TokenResponse.Error)
            assertEquals("invalid_request", (response as TokenResponse.Error).error.error)
            assertEquals("Invalid request", response.error.errorDescription)
        }
    }
}
