package no.nav.helse.fritakagp.processing.brukernotifikasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.GravidKravMetrics
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import org.apache.kafka.common.record.Record
import java.lang.IllegalArgumentException
import java.util.*

class BrukernotifikasjonProcessor(
    private val gravidKravRepo: GravidKravRepository,
    private val gravidSoeknadRepo: GravidSoeknadRepository,
    private val kroniskKravRepo: KroniskKravRepository,
    private val kronsikSoeknadRepo: KroniskSoeknadRepository,
    private val om: ObjectMapper
) : BakgrunnsjobbProsesserer {

    companion object {
        val JOB_TYPE = "brukernotifikasjon"
    }
    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        val jobbData = om.readValue<Jobbdata>(jobb.data)
        val kafkaMelding = map(jobbData)


        GravidKravMetrics.tellKvitteringSendt()
    }

    private fun map(jobbData: Jobbdata): String {

        return when(jobbData.skjemaType) {
            Jobbdata.SkjemaType.KroniskKrav ->""
            Jobbdata.SkjemaType.KroniskSøknad -> ""
            Jobbdata.SkjemaType.GravidKrav -> ""
            Jobbdata.SkjemaType.GravidSøknad -> TODO()
        }
    }


    data class Jobbdata(
        val skjemaId: UUID,
        val skjemaType: SkjemaType
    ) {
        enum class SkjemaType {
            KroniskKrav,
            KroniskSøknad,
            GravidKrav,
            GravidSøknad
        }
    }
}
