package no.nav.helse.fritakagp.db

import no.nav.helse.fritakagp.domain.SoeknadGravid
import java.util.*

class MockSoeknadRepo : Repository{
    private val soeknadListe = mutableListOf<SoeknadGravid>()

    override fun insert(soeknad: SoeknadGravid): UUID {
        soeknadListe.add(soeknad)
        return soeknad.id
    }

    override fun delete(id: UUID): Int {
        TODO("Not yet implemented")
    }

    override fun getById(id: UUID): SoeknadGravid? {
        TODO("Not yet implemented")
    }
}