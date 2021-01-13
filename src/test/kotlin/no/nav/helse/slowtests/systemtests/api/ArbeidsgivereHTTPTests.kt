package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.KroniskTestData
import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnOrganisasjon
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ArbeidsgivereHTTPTests : SystemTestBase() {
    private val arbeidsgivereUrl = "/api/v1/arbeidsgivere"


    @Test
    fun `Skal returnere 401 når man ikke er logget inn`() = suspendableTest {
        val response = httpClient.get<HttpResponse> {
            appUrl(arbeidsgivereUrl)
            contentType(ContentType.Application.Json)
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `Skal returnere liste av arbeidsgivere når man er logget inn`() = suspendableTest {
        val response = httpClient.get<Set<AltinnOrganisasjon>> {
            appUrl(arbeidsgivereUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = KroniskTestData.kroniskSoknadMedFil
        }

        Assertions.assertThat(response.size).isGreaterThan(0)
    }
}