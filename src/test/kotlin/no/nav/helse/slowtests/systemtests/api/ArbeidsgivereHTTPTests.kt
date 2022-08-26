package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.KroniskTestData
import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnOrganisasjon
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ArbeidsgivereHTTPTests : SystemTestBase() {
    private val arbeidsgivereUrl = "/api/v1/arbeidsgivere"

    @Test
    fun `Skal returnere 401 når man ikke er logget inn`() = suspendableTest {
        val exception = assertThrows<ClientRequestException>
        {
            httpClient.get<HttpResponse> {
                appUrl(arbeidsgivereUrl)
                contentType(ContentType.Application.Json)
            }
        }

        Assertions.assertThat(exception.response.status).isEqualTo(HttpStatusCode.Unauthorized)
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
