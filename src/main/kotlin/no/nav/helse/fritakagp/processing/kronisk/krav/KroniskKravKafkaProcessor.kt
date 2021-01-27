package no.nav.helse.fritakagp.processing.kronisk.krav

import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import no.nav.helse.fritakagp.integration.kafka.SoeknadsmeldingKafkaProducer
import org.slf4j.LoggerFactory
import java.util.*

class KroniskKravKafkaProcessor(
    private val kroniskKrav: KroniskKrav,
    private val kafkaProducer: SoeknadsmeldingKafkaProducer
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "kronisk-krav-send-kafka"
    }

    val log = LoggerFactory.getLogger(KroniskKravKafkaProcessor::class.java)

    /**
     * Sender kroniskkrav til Kafka kø
     */
    override fun prosesser(jobbDataString: String) {
        val retRecord = kafkaProducer.sendMessage(kroniskKrav)
        log.info("Skrevet ${kroniskKrav.id} til Kafka til topic ${retRecord!!.topic()}")
    }

    data class JobbData(val id: UUID)
}