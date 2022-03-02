package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.GravidTestData
import no.nav.helse.KroniskTestData
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.domain.GravidKrav
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.test.inject

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
    fun `Skal returnere forbidden hvis virksomheten ikke er i auth listen fra altinn`() = suspendableTest {
        val repo by inject<KroniskKravRepository>()

        repo.insert(KroniskTestData.kroniskKrav)
        val responseExcepion = assertThrows<ClientRequestException> {
            httpClient.delete<HttpResponse> {
                appUrl("$kravKroniskUrl/${KroniskTestData.kroniskKrav.id}")
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
            }
        }
        assertThat(responseExcepion.response.status).isEqualTo(HttpStatusCode.Forbidden)
    }
}
