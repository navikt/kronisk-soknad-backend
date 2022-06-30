package no.nav.helse.fritakagp.integration

import io.mockk.mockk
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.AccessTokenProvider
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.whoami
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URL

class ArbeidsgiverNotifikasjonKlientTest : SystemTestBase() {
    private val tokenClient = mockk<AccessTokenProvider>(relaxed = true)

    @Test
    fun `Gyldig whoami`() = suspendableTest {
        val client = ArbeidsgiverNotifikasjonKlient(
            URL("https://notifikasjon-fake-produsent-api.labs.nais.io/"),
            tokenClient,
            httpClient
        )
        val resultat = client.whoami()
        Assertions.assertThat(resultat).hasSizeGreaterThan(10)
    }
}
