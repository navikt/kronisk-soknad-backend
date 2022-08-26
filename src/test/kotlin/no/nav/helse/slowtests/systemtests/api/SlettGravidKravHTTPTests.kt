package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.db.GravidKravRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.test.inject
import java.util.UUID

class SlettGravidKravHTTPTests : SystemTestBase() {
    private val kravGravidUrl = "/api/v1/gravid/krav"

    @Test
    internal fun `Skal returnere 200 OK når vi sletter med korrekt bruker innlogget`() = suspendableTest {
        val repo by inject<GravidKravRepository>()

        repo.insert(GravidTestData.gravidKrav)

        val response = httpClient.delete<HttpResponse> {
            appUrl("$kravGravidUrl/${GravidTestData.gravidKrav.id}")
            contentType(ContentType.Application.Json)
            loggedInAs(GravidTestData.gravidKrav.identitetsnummer)
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    internal fun `Skal returnere 404 når kravet ikke finnes`() = suspendableTest {
        val repo by inject<GravidKravRepository>()

        repo.insert(GravidTestData.gravidKrav)

        val responseExcepion = assertThrows<ClientRequestException> {
            httpClient.delete<HttpResponse> {
                appUrl("$kravGravidUrl/${UUID.randomUUID()}")
                contentType(ContentType.Application.Json)
                loggedInAs(GravidTestData.gravidKrav.identitetsnummer)
            }
        }

        assertThat(responseExcepion.response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `Skal returnere forbidden hvis virksomheten ikke er i auth listen fra altinn`() = suspendableTest {
        val repo by inject<GravidKravRepository>()

        val id = UUID.randomUUID()

        repo.insert(GravidTestData.gravidKrav.copy(virksomhetsnummer = "123456785", id = id))
        val responseExcepion = assertThrows<ClientRequestException> {
            httpClient.delete<HttpResponse> {
                appUrl("$kravGravidUrl/$id")
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
            }
        }
        assertThat(responseExcepion.response.status).isEqualTo(HttpStatusCode.Forbidden)
    }
}
