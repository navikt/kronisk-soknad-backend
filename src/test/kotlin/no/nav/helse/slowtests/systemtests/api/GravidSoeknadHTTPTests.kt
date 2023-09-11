package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.call.body
import io.ktor.client.call.receive
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.domain.GravidSoeknad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.core.component.inject

class GravidSoeknadHTTPTests : SystemTestBase() {
    private val soeknadGravidUrl = "/api/v1/gravid/soeknad"

    @Test
    internal fun `Returnerer søknaden når korrekt bruker er innlogget, 404 når ikke`() = suspendableTest {
        val repo by inject<GravidSoeknadRepository>()

        repo.insert(GravidTestData.soeknadGravid)

        val exception = assertThrows<ClientRequestException>
        {
            httpClient.get {
                appUrl("$soeknadGravidUrl/${GravidTestData.soeknadGravid.id}")
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
            }
        }

        assertThat(exception.response.status).isEqualTo(HttpStatusCode.NotFound)

        val accessGrantedForm = httpClient.get {
            appUrl("$soeknadGravidUrl/${GravidTestData.soeknadGravid.id}")
            contentType(ContentType.Application.Json)
            loggedInAs(GravidTestData.soeknadGravid.identitetsnummer)
        }

        assertThat(accessGrantedForm).isEqualToIgnoringGivenFields(GravidTestData.soeknadGravid, "referansenummer")
    }

    @Test
    fun `invalid enum fields gives 400 Bad request`() = suspendableTest {
        val exception = assertThrows<ClientRequestException>
        {
            httpClient.post {
                appUrl(soeknadGravidUrl)
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")

                setBody("""
                {
                    "fnr": "${GravidTestData.validIdentitetsnummer}",
                    "orgnr": "${GravidTestData.fullValidSoeknadRequest.virksomhetsnummer}",
                    "tilrettelegge": true,
                    "tiltak": ["IKKE GYLDIG"]
                }
                """.trimIndent())
            }
        }

        assertThat(exception.response.status).isEqualTo(HttpStatusCode.BadRequest)
    }

    @Test
    fun `Skal returnere Created ved feilfritt skjema uten fil`() = suspendableTest {
        val response = httpClient.post {
            appUrl(soeknadGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            setBody(GravidTestData.fullValidSoeknadRequest)
        }

        val soeknad = response.body<GravidSoeknad>()
        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        assertThat(soeknad.virksomhetsnummer).isEqualTo(GravidTestData.fullValidSoeknadRequest.virksomhetsnummer)
    }

    @Test
    fun `Skal returnere Created når fil er vedlagt`() = suspendableTest {
        val response = httpClient.post {
            appUrl(soeknadGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            setBody(GravidTestData.gravidSoknadMedFil)
        }

        val soeknad = response.body<GravidSoeknad>()
        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        assertThat(soeknad.harVedlegg).isEqualTo(true)
    }
}
