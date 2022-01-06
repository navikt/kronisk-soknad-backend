package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
            httpClient.get<HttpResponse> {
                appUrl("$soeknadGravidUrl/${GravidTestData.soeknadGravid.id}")
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
            }
        }

        assertThat(exception.response.status).isEqualTo(HttpStatusCode.NotFound)

        val accessGrantedForm = httpClient.get<GravidSoeknad> {
            appUrl("$soeknadGravidUrl/${GravidTestData.soeknadGravid.id}")
            contentType(ContentType.Application.Json)
            loggedInAs(GravidTestData.soeknadGravid.identitetsnummer)
        }

        assertThat(accessGrantedForm).isEqualTo(GravidTestData.soeknadGravid)
    }

    @Test
    fun `invalid enum fields gives 400 Bad request`() = suspendableTest {
        val exception = assertThrows<ClientRequestException>
        {
            httpClient.post<HttpResponse> {
                appUrl(soeknadGravidUrl)
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")

                body = """
                {
                    "fnr": "${GravidTestData.validIdentitetsnummer}",
                    "orgnr": "${GravidTestData.fullValidSoeknadRequest.virksomhetsnummer}",
                    "tilrettelegge": true,
                    "tiltak": ["IKKE GYLDIG"]
                }
                """.trimIndent()
            }
        }

        assertThat(exception.response.status).isEqualTo(HttpStatusCode.BadRequest)
    }

    @Test
    fun `Skal returnere Created ved feilfritt skjema uten fil`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(soeknadGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.fullValidSoeknadRequest
        }

        val soeknad = response.receive<GravidSoeknad>()
        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        assertThat(soeknad.virksomhetsnummer).isEqualTo(GravidTestData.fullValidSoeknadRequest.virksomhetsnummer)
    }

    @Test
    fun `Skal returnere Created når fil er vedlagt`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(soeknadGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.gravidSoknadMedFil
        }

        val soeknad = response.receive<GravidSoeknad>()
        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        assertThat(soeknad.harVedlegg).isEqualTo(true)
    }
}
