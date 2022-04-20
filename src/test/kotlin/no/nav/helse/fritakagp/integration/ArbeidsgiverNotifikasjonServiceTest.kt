package no.nav.helse.fritakagp.integration

import io.mockk.mockk
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.arbeidsgiverNotifikasjon.ArbeidsgiverNotifikasjonKlientImpl
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ArbeidsgiverNotifikasjonKlientTest : SystemTestBase() {
    private val tokenClient = mockk<AccessTokenProvider>(relaxed = true)

    @Test
    fun `Gyldig whoami`() = suspendableTest {
        val client = ArbeidsgiverNotifikasjonKlientImpl(
            "https://notifikasjon-fake-produsent-api.labs.nais.io/",
            httpClient,
            tokenClient
        )
        val resultat = client.whoami()
        Assertions.assertThat(resultat).hasSizeGreaterThan(10)
    }
}
