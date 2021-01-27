package no.nav.helse.fritakagp.processing.gravid.soeknad

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.GravidSoeknadMetrics
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.util.*

class GravidSoeknadKvitteringProcessor(
    private val gravidSoeknadKvitteringSender: GravidSoeknadKvitteringSender,
    private val db: GravidSoeknadRepository,
    private val om: ObjectMapper
) : BakgrunnsjobbProsesserer {

    companion object {
        val JOB_TYPE = "gravid-søknad-altinn-kvittering"
    }

    override fun prosesser(jobbData: String) {
        val kvitteringJobbData = om.readValue(jobbData, Jobbdata::class.java)
        val soeknad = db.getById(kvitteringJobbData.soeknadId)
            ?: throw IllegalArgumentException("Fant ikke søknaden i jobbdatanene $jobbData")

        gravidSoeknadKvitteringSender.send(soeknad)
        GravidSoeknadMetrics.tellKvitteringSendt()
    }

    data class Jobbdata(
        val soeknadId: UUID
    )
}
