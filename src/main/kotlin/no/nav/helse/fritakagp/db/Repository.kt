package no.nav.helse.fritakagp.db

import no.nav.helse.fritakagp.domain.SoeknadGravid
import java.util.*

interface Repository {
    fun insert(soeknad: SoeknadGravid): UUID
    fun delete(id: UUID): Int
    fun getById(id: UUID): SoeknadGravid?
}