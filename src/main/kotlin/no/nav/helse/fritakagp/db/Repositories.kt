package no.nav.helse.fritakagp.db

import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.domain.KroniskSoeknad

interface GravidKravRepository: SimpleJsonbRepository<GravidKrav>

interface KroniskSoeknadRepository: SimpleJsonbRepository<KroniskSoeknad>

interface GravidSoeknadRepository: SimpleJsonbRepository<GravidSoeknad>