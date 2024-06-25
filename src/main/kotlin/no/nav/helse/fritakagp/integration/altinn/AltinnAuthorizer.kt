package no.nav.helse.fritakagp.integration.altinn

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.altinn.AltinnClient

fun AltinnClient.hasAccess(identitetsnummer: String, orgnr: String): Boolean {
    return runBlocking {
        harRettighetForOrganisasjon(identitetsnummer, orgnr)
    }
}
