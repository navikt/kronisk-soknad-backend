package no.nav.helse.fritakagp.processing.kvittering

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.util.*

class KvitteringProcessor(
        val kvitteringSender: KvitteringSender,
        val db: GravidSoeknadRepository,
        val om: ObjectMapper
) : BakgrunnsjobbProsesserer {

    companion object {
        val JOB_TYPE = "kvittering"
    }

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return forrigeForsoek.plusHours(1)
    }

    override fun prosesser(jobbData: String) {
        val kvitteringJobbData = om.readValue(jobbData, KvitteringJobData::class.java)
        val soeknad = db.getById(kvitteringJobbData.soeknadId)
            ?: throw IllegalArgumentException("Fant ikke søknaden i jobbdatanene $jobbData")

        kvitteringSender.send(soeknad)
    }
}

data class KvitteringJobData(
        val soeknadId: UUID
)