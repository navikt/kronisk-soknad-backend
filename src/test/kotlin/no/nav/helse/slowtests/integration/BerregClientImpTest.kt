package no.nav.helse.slowtests.integration

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.features.*
import no.nav.helse.fritakagp.integration.brreg.BerregClientImp
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class BrregClientImpTest  : SystemTestBase() {
    private val enhetsUrl ="data.brreg.no/enhetsregisteret/api/enheter"

    @Test
    fun `Skal returnere ARBEIDS- OG VELFERDSETATEN`() = suspendableTest {
        val om = ObjectMapper()
        val client = BerregClientImp(httpClient, om, enhetsUrl)
        val navn = client.getVirksomhetsNavn("889640782")
        Assertions.assertThat(navn).isEqualTo("ARBEIDS- OG VELFERDSETATEN")
    }

    @Test
    fun `Skal returnere 404`() = suspendableTest {
        val om = ObjectMapper()
        val fakeOrgNr = "123456789"
        val errmsg = "Client request(https://${enhetsUrl}/$fakeOrgNr) invalid: 404"
        val client = BerregClientImp(httpClient, om, enhetsUrl)
        val exception = assertFailsWith<ClientRequestException> { client.getVirksomhetsNavn(fakeOrgNr) }
        Assertions.assertThat(exception.message).contains(errmsg)
    }
}