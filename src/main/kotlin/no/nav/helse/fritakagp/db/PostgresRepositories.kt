package no.nav.helse.fritakagp.db

import no.nav.helse.fritakagp.customObjectMapper
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import javax.sql.DataSource

class PostgresGravidSoeknadRepository(ds: DataSource) : GravidSoeknadRepository,
    SimpleJsonbRepositoryBase<GravidSoeknad>("soeknadgravid", ds, customObjectMapper(), GravidSoeknad::class.java)

class PostgresKroniskSoeknadRepository(ds: DataSource) : KroniskSoeknadRepository,
    SimpleJsonbRepositoryBase<KroniskSoeknad>("soeknadkronisk", ds, customObjectMapper(), KroniskSoeknad::class.java)

class PostgresGravidKravRepository(ds: DataSource) : GravidKravRepository,
    SimpleJsonbRepositoryBase<GravidKrav>("kravgravid", ds, customObjectMapper(), GravidKrav::class.java)

class PostgresKroniskKravRepository(ds: DataSource) : KroniskKravRepository,
    SimpleJsonbRepositoryBase<KroniskKrav>("krav_kronisk", ds, customObjectMapper(), KroniskKrav::class.java)
