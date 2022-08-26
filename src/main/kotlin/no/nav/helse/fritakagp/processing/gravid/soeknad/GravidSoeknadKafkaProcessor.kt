package no.nav.helse.fritakagp.processing.gravid.soeknad

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.integration.kafka.SoeknadmeldingSender
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class GravidSoeknadKafkaProcessor(
    private val gravidSoeknadRepo: GravidSoeknadRepository,
    private val om: ObjectMapper,
    private val kafkaProducer: SoeknadmeldingSender
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "gravid-søknad-send-kafka"
    }
    override val type: String get() = JOB_TYPE

    private val logger = this.logger()

    /**
     * Sender gravidsoeknad til Kafka kø
     */
    override fun prosesser(jobb: Bakgrunnsjobb) {
        val jobbData = om.readValue<JobbData>(jobb.data)
        val gravidSoeknad = gravidSoeknadRepo.getById(jobbData.id)
            ?: throw IllegalStateException("${jobbData.id} fantes ikke")

        val retRecord = kafkaProducer.sendMessage(gravidSoeknad)
        logger.info("Skrevet ${gravidSoeknad.id} til Kafka til topic ${retRecord!!.topic()}")
    }

    data class JobbData(val id: UUID)
}
