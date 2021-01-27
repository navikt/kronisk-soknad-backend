package no.nav.helse.fritakagp.processing.gravid.krav

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.GravidKravMetrics
import no.nav.helse.fritakagp.db.GravidKravRepository
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.util.*

class GravidKravKvitteringProcessor(
    private val gravidKravKvitteringSender: GravidKravKvitteringSender,
    private val db: GravidKravRepository,
    private val om: ObjectMapper
) : BakgrunnsjobbProsesserer {

    companion object {
        val JOB_TYPE = "gravid-krav-altinn-kvittering"
    }

    override fun prosesser(jobbData: String) {
        val kvitteringJobbData = om.readValue(jobbData, Jobbdata::class.java)
        val krav = db.getById(kvitteringJobbData.kravId)
            ?: throw IllegalArgumentException("Fant ikke kravet i jobbdatanene $jobbData")

        gravidKravKvitteringSender.send(krav)
        GravidKravMetrics.tellKvitteringSendt()
    }

    data class Jobbdata(
        val kravId: UUID
    )
}
