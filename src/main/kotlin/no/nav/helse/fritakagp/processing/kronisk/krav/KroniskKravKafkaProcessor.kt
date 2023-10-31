package no.nav.helse.fritakagp.processing.kronisk.krav

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.integration.kafka.KravmeldingSender
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class KroniskKravKafkaProcessor(
    private val kroniskKravRepo: KroniskKravRepository,
    private val kafkaProducer: KravmeldingSender,
    private val om: ObjectMapper
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "kronisk-krav-send-kafka"
    }

    override val type: String get() = JOB_TYPE

    private val logger = this.logger()

    /**
     * Sender kroniskkrav til Kafka kø
     */
    override fun prosesser(jobb: Bakgrunnsjobb) {
        val jobbData = om.readValue<JobbData>(jobb.data)
        val kroniskKrav = kroniskKravRepo.getById(jobbData.id) ?: throw java.lang.IllegalStateException("${jobbData.id} fantes ikke")
        val retRecord = kafkaProducer.sendMessage(kroniskKrav)
        logger.info("Skrevet ${kroniskKrav.id} til Kafka til topic ${retRecord!!.topic()}")
    }

    data class JobbData(val id: UUID)
}
