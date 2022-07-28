package no.nav.helse.fritakagp.integration.altinn

import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnOrganisasjon
import no.nav.helse.arbeidsgiver.utils.SimpleHashMapCache
import no.nav.helse.arbeidsgiver.web.auth.AltinnOrganisationsRepository
import no.nav.helse.fritakagp.integration.use
import java.time.Duration

class CachedAuthRepo(
    private val sourceRepo: AltinnOrganisationsRepository,
) : AltinnOrganisationsRepository {
    private val cache = SimpleHashMapCache<Set<AltinnOrganisasjon>>(Duration.ofMinutes(60), 100)

    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> =
        cache.use(identitetsnummer) {
            sourceRepo.hentOrgMedRettigheterForPerson(identitetsnummer)
        }
}
