package no.nav.helse.fritakagp.processing.kronisk.soeknad

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helse.fritakagp.integration.kafka.SoeknadmeldingSender
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class KroniskSoeknadKafkaProcessor(
    private val kroniskSoeknadRepo: KroniskSoeknadRepository,
    private val kafkaProducer: SoeknadmeldingSender,
    private val om: ObjectMapper
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "kronisk-søknad-send-kafka"
    }

    override val type: String get() = JOB_TYPE

    private val logger = this.logger()

    /**
     * Sender kronisksoeknad til Kafka kø
     */
    override fun prosesser(jobb: Bakgrunnsjobb) {
        val jobbData = om.readValue<JobbData>(jobb.data)
        val kroniskSoeknad = kroniskSoeknadRepo.getById(jobbData.id) ?: throw java.lang.IllegalStateException("${jobbData.id} fantes ikke")
        val retRecord = kafkaProducer.sendMessage(kroniskSoeknad)
        logger.info("Skrevet ${kroniskSoeknad.id} til Kafka til topic ${retRecord!!.topic()}")
    }

    data class JobbData(val id: UUID)
}
