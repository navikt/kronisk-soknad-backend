package no.nav.helse.slowtests.integration

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.fritakagp.integration.brreg.BrregClientImp
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class BrregClientImpTest  : SystemTestBase() {
    private val enhetsUrl ="data.brreg.no/enhetsregisteret/api/underenheter"

    @Test
    fun `Skal returnere ARBEIDS- OG VELFERDSETATEN`() = suspendableTest {
        val om = ObjectMapper()
        val client = BrregClientImp(httpClient, om, enhetsUrl)
        val navn = client.getVirksomhetsNavn("996727750")
        Assertions.assertThat(navn).isEqualTo("NESODDEN KOMMUNE TEKNISK SERVICE")
    }

    @Test
    fun `Skal returnere Ukjent arbeidsgiver`() = suspendableTest {
        val om = ObjectMapper()
        val client = BrregClientImp(httpClient, om, enhetsUrl)
        val navn = client.getVirksomhetsNavn("123456789")
        Assertions.assertThat(navn).isEqualTo("Ukjent arbeidsgiver")
    }
}