package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.KroniskTestData
import no.nav.helse.fritakagp.db.KroniskKravRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.inject
import java.util.*

class SlettKroniskKravHTTPTests : SystemTestBase() {
    private val kravKroniskUrl = "/api/v1/kronisk/krav"

    @Test
    internal fun `Skal returnere 200 OK når vi sletter med korrekt bruker innlogget`() = suspendableTest {
        val repo by inject<KroniskKravRepository>()

        repo.insert(KroniskTestData.kroniskKrav)

        val response = httpClient.delete<HttpResponse> {
            appUrl("$kravKroniskUrl/${KroniskTestData.kroniskKrav.id}")
            contentType(ContentType.Application.Json)
            loggedInAs(KroniskTestData.kroniskKrav.identitetsnummer)
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    internal fun `Skal returnere 404 når kravet ikke finnes`() = suspendableTest {
        val repo by inject<KroniskKravRepository>()

        repo.insert(KroniskTestData.kroniskKrav)

        val responseExcepion = assertThrows<ClientRequestException> {
            httpClient.delete<HttpResponse> {
                appUrl("$kravKroniskUrl/${UUID.randomUUID()}")
                contentType(ContentType.Application.Json)
                loggedInAs(KroniskTestData.kroniskKrav.identitetsnummer)
            }
        }

        assertThat(responseExcepion.response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `Skal returnere forbidden hvis virksomheten ikke er i auth listen fra altinn`() = suspendableTest {
        val repo by inject<KroniskKravRepository>()

        val id = UUID.randomUUID()

        repo.insert(KroniskTestData.kroniskKrav.copy(virksomhetsnummer = "123456785", id = id))
        val responseExcepion = assertThrows<ClientRequestException> {
            httpClient.delete<HttpResponse> {
                appUrl("$kravKroniskUrl/$id")
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
            }
        }
        assertThat(responseExcepion.response.status).isEqualTo(HttpStatusCode.Forbidden)
    }
}
