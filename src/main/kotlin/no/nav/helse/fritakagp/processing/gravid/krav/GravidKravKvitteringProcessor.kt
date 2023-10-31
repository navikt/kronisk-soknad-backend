package no.nav.helse.fritakagp.processing.gravid.krav

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.GravidKravMetrics
import no.nav.helse.fritakagp.db.GravidKravRepository
import java.util.UUID

class GravidKravKvitteringProcessor(
    private val gravidKravKvitteringSender: GravidKravKvitteringSender,
    private val db: GravidKravRepository,
    private val om: ObjectMapper
) : BakgrunnsjobbProsesserer {

    companion object {
        val JOB_TYPE = "gravid-krav-altinn-kvittering"
    }

    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        val kvitteringJobbData = om.readValue(jobb.data, Jobbdata::class.java)
        val krav = db.getById(kvitteringJobbData.kravId)
            ?: throw IllegalArgumentException("Fant ikke kravet i jobbdataene ${jobb.data}")

        gravidKravKvitteringSender.send(krav)
        GravidKravMetrics.tellKvitteringSendt()
    }

    data class Jobbdata(
        val kravId: UUID
    )
}
