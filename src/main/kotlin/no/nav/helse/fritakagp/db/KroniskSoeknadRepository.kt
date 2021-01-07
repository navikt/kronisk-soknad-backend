package no.nav.helse.fritakagp.db

import no.nav.helse.fritakagp.domain.SoeknadKronisk
import java.sql.Connection
import java.util.*

interface KroniskSoeknadRepository {
    fun insert(soeknad: SoeknadKronisk): UUID
    fun insert(soeknad: SoeknadKronisk, connection: Connection): UUID
    fun delete(id: UUID): Int
    fun update(soeknad: SoeknadKronisk)
    fun getById(id: UUID): SoeknadKronisk?
}
