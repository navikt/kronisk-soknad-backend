package no.nav.helse.fritakagp.processing.kronisk.soeknad

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.KroniskSoeknadMetrics
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.util.*

class KroniskSoeknadKvitteringProcessor(
    private val kroniskSoeknadKvitteringSender: KroniskSoeknadKvitteringSender,
    private val db: KroniskSoeknadRepository,
    private val om: ObjectMapper
) : BakgrunnsjobbProsesserer {

    companion object {
        val JOB_TYPE = "kvitt_søk_kronisk"
    }

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return forrigeForsoek.plusHours(1)
    }

    override fun prosesser(jobbData: String) {
        val kvitteringJobbData: Jobbdata = om.readValue(jobbData)
        val soeknad = db.getById(kvitteringJobbData.soeknadId)
            ?: throw IllegalArgumentException("Fant ikke søknaden i jobbdatanene $jobbData")

        kroniskSoeknadKvitteringSender.send(soeknad)
        KroniskSoeknadMetrics.tellKvitteringSendt()
    }

    data class Jobbdata(
        val soeknadId: UUID
    )
}
