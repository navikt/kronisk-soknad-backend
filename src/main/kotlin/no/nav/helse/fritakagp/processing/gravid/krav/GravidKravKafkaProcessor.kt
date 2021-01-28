package no.nav.helse.fritakagp.processing.gravid.krav

import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.integration.kafka.KravmeldingSender
import org.slf4j.LoggerFactory
import java.util.*

class GravidKravKafkaProcessor(
    private val gravidKrav: GravidKrav,
    private val kafkaProducer: KravmeldingSender
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "gravid-krav-send-kafka"
    }

    val log = LoggerFactory.getLogger(GravidKravKafkaProcessor::class.java)

    /**
     * Sender gravidkrav til Kafka kø
     */
    override fun prosesser(jobbDataString: String) {
        val retRecord = kafkaProducer.sendMessage(gravidKrav)
        log.info("Skrevet ${gravidKrav.id} til Kafka til topic ${retRecord!!.topic()}")
    }

    data class JobbData(val id: UUID)
}