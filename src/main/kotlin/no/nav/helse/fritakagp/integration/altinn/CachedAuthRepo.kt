package no.nav.helse.fritakagp.integration.altinn

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.altinn.AltinnClient
import no.nav.helsearbeidsgiver.altinn.AltinnOrganisasjon
import no.nav.helsearbeidsgiver.utils.cache.LocalCache
import kotlin.time.Duration.Companion.minutes

interface AltinnRepo {
    fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon>
}
class CachedAuthRepo(
    private val altinnClient: AltinnClient
) : AltinnRepo {
    private val cache = LocalCache<Set<AltinnOrganisasjon>>(60.minutes, 100)

    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> =
        runBlocking {
            cache.get(identitetsnummer) {
                altinnClient.hentRettighetOrganisasjoner(identitetsnummer)
            }
        }
}
