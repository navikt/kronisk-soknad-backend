package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.KroniskTestData
import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnOrganisasjon
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ArbeidsgivereHTTPTests : SystemTestBase() {
    private val arbeidsgivereUrl = "/fritak-agp-api/api/v1/arbeidsgivere"

    @Test
    fun `Skal returnere 401 når man ikke er logget inn`() = suspendableTest {
        val status = httpClient.get {
            appUrl(arbeidsgivereUrl)
            contentType(ContentType.Application.Json)
        }.status

        Assertions.assertThat(status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `Skal returnere liste av arbeidsgivere når man er logget inn`() = suspendableTest {
        val response: Set<AltinnOrganisasjon> = httpClient.get {
            appUrl(arbeidsgivereUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            setBody(KroniskTestData.kroniskSoknadMedFil)
        }.body()

        Assertions.assertThat(response.size).isGreaterThan(0)
    }
}
