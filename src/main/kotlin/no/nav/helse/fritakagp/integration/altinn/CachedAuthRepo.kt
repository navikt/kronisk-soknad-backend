package no.nav.helse.fritakagp.integration.altinn

import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnOrganisasjon
import no.nav.helse.arbeidsgiver.web.auth.AltinnOrganisationsRepository
import no.nav.helsearbeidsgiver.utils.cache.LocalCache
import kotlin.time.Duration.Companion.minutes

class CachedAuthRepo(
    private val sourceRepo: AltinnOrganisationsRepository,
) : AltinnOrganisationsRepository {
    private val cache = LocalCache<Set<AltinnOrganisasjon>>(60.minutes, 100)

    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> =
        cache.get(identitetsnummer) {
            sourceRepo.hentOrgMedRettigheterForPerson(identitetsnummer)
        }
}
