package no.nav.helse.fritakagp.processing.kronisk.krav

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.KroniskKravMetrics
import no.nav.helse.fritakagp.db.KroniskKravRepository
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.util.*

class KroniskKravKvitteringProcessor(
    private val kroniskKravKvitteringSender: KroniskKravKvitteringSender,
    private val db: KroniskKravRepository,
    private val om: ObjectMapper
) : BakgrunnsjobbProsesserer {

    companion object {
        val JOB_TYPE = "kronisk-krav-altinn-kvittering"
    }

    override fun prosesser(jobbData: String) {
        val kvitteringJobbData = om.readValue(jobbData, Jobbdata::class.java)
        val krav = db.getById(kvitteringJobbData.kravId)
            ?: throw IllegalArgumentException("Fant ikke kravet i jobbdatanene $jobbData")

        kroniskKravKvitteringSender.send(krav)
        KroniskKravMetrics.tellKvitteringSendt()
    }

    data class Jobbdata(
        val kravId: UUID
    )
}
