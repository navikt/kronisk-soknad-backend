package no.nav.helse.fritakagp.db

import no.nav.helse.fritakagp.domain.SoeknadGravid
import java.sql.Connection
import java.util.*

interface GravidSoeknadRepository {
    fun insert(soeknad: SoeknadGravid): UUID
    fun insert(soeknad: SoeknadGravid, connection: Connection): UUID
    fun delete(id: UUID): Int
    fun update(soeknad: SoeknadGravid)
    fun getById(id: UUID): SoeknadGravid?
}