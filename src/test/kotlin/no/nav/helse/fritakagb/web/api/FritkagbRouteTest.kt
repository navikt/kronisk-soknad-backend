package no.nav.helse.fritakagb.web.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.helse.TestData
import no.nav.helse.fritakagb.web.integration.ControllerIntegrationTestBase
import no.nav.helse.fritakagp.web.fritakModule
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.koin.ktor.ext.get

class FritkagbRouteTest : ControllerIntegrationTestBase() {
    @KtorExperimentalAPI
    @Test
    fun `Skjek at return value er ok`() {
        configuredTestApplication({
            fritakModule()
        }) {
            val om = application.get<ObjectMapper>()
            doAuthenticatedRequest(HttpMethod.Post, "api/v1/gravid/soeknad") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                        om.writeValueAsString(TestData.gravidSoknad)
                )
            }.apply {
                Assertions.assertEquals(response.status(), HttpStatusCode.OK)

            }
        }
    }
}