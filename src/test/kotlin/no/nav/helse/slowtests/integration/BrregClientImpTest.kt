package no.nav.helse.slowtests.integration

import no.nav.helse.fritakagp.integration.brreg.BrregClientImpl
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class BrregClientImpTest : SystemTestBase() {
    private val enhetsUrl = "https://data.brreg.no/enhetsregisteret/api/underenheter"

    private val nesoddenKommuneVirksomhet = "996727750"
    private val nesoddenKommuneOrgLedd = "974637685"

    @Test
    fun `Skal returnere riktig navn`() = suspendableTest {
        val client = BrregClientImpl(httpClient, enhetsUrl)
        val navn = client.getVirksomhetsNavn(nesoddenKommuneVirksomhet)
        Assertions.assertThat(navn).isEqualTo("NESODDEN KOMMUNE TEKNISK SERVICE")
    }

    @Test
    fun `Skal returnere true for virksomhetsnummer`() = suspendableTest {
        val client = BrregClientImpl(httpClient, enhetsUrl)
        val erVirksomhet = client.erVirksomhet(nesoddenKommuneVirksomhet)
        Assertions.assertThat(erVirksomhet).isTrue()
    }

    @Test
    fun `Skal returnere false for juridisk orgledd`() = suspendableTest {
        val client = BrregClientImpl(httpClient, enhetsUrl)
        val erVirksomhet = client.erVirksomhet(nesoddenKommuneOrgLedd)
        Assertions.assertThat(erVirksomhet).isFalse()
    }

    @Test
    fun `Skal returnere OSLO TAXI AS`() = suspendableTest {
        val client = BrregClientImpl(httpClient, enhetsUrl)
        val navn = client.getVirksomhetsNavn("976172035")
        Assertions.assertThat(navn).isEqualTo("OSLO TAXI AS")
    }

    @Test
    fun `Skal returnere Ukjent arbeidsgiver`() = suspendableTest {
        val client = BrregClientImpl(httpClient, enhetsUrl)
        val navn = client.getVirksomhetsNavn("123456789")
        Assertions.assertThat(navn).isEqualTo("Ukjent arbeidsgiver")
    }
}
