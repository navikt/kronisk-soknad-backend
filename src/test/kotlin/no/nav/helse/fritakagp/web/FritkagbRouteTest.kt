package no.nav.helse.fritakagp.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.helse.TestData
import no.nav.helse.TestDataMedFil
import no.nav.helse.fritakagp.web.integration.ControllerIntegrationTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.koin.ktor.ext.get

class FritkagbRouteTest : ControllerIntegrationTestBase() {
    //val gcpBucket = mockk<BucketUtils>()
    @KtorExperimentalAPI
    @Test
    //@Disabled
    fun `Skjek soeknad uten fil`() {
        configuredTestApplication({
            fritakModule()
        }) {
            val om = application.get<ObjectMapper>()
            doAuthenticatedRequest(HttpMethod.Post, "api/v1/gravid/soeknad") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                        om.writeValueAsString(TestData.soeknadGravid)
                )
            }.apply {
                Assertions.assertEquals(response.status(), HttpStatusCode.OK)
            }
        }
    }

    @KtorExperimentalAPI
    @Test
    @Disabled
    fun `Skjek soeknad med fil`() {
        configuredTestApplication({
            fritakModule()
        }) {
            val om = application.get<ObjectMapper>()
            doAuthenticatedRequest(HttpMethod.Post, "api/v1/gravid/soeknad") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                        om.writeValueAsString(TestDataMedFil.gravidSoknadMedFil)
                )
            }.apply {
                Assertions.assertEquals(response.status(), HttpStatusCode.OK)
            }
        }
    }
}