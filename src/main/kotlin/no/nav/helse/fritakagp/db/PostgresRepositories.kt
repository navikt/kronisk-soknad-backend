package no.nav.helse.fritakagp.db

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import java.util.*
import javax.sql.DataSource

class PostgresGravidSoeknadRepository(ds: DataSource, om: ObjectMapper) : GravidSoeknadRepository,
    SimpleJsonbRepositoryBase<GravidSoeknad>("soeknadgravid", ds, om, GravidSoeknad::class.java)

class PostgresKroniskSoeknadRepository(ds: DataSource, om: ObjectMapper) : KroniskSoeknadRepository,
    SimpleJsonbRepositoryBase<KroniskSoeknad>("soeknadkronisk", ds, om, KroniskSoeknad::class.java)

class PostgresGravidKravRepository(ds: DataSource, om: ObjectMapper) : GravidKravRepository,
    SimpleJsonbRepositoryBase<GravidKrav>("kravgravid", ds, om, GravidKrav::class.java)

class PostgresKroniskKravRepository(ds: DataSource, om: ObjectMapper) : KroniskKravRepository,
    SimpleJsonbRepositoryBase<KroniskKrav>("krav_kronisk", ds, om, KroniskKrav::class.java)
