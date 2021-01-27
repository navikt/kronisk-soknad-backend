package no.nav.helse.fritakagp.processing.kronisk.soeknad

import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import no.nav.helse.fritakagp.integration.kafka.SoeknadsmeldingKafkaProducer
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class KroniskSoeknadKafkaProcessor(
    private val kroniskSoeknad: KroniskSoeknad,
    private val kafkaProducer: SoeknadsmeldingKafkaProducer
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "kronisk-søknad-send-kafka"
    }

    val log = LoggerFactory.getLogger(KroniskSoeknadKafkaProcessor::class.java)

    /**
     * Sender kronisksoeknad til Kafka kø
     */
    override fun prosesser(jobbDataString: String) {
        val retRecord = kafkaProducer.sendMessage(kroniskSoeknad)
        log.info("Skrevet ${kroniskSoeknad.id} til Kafka til topic ${retRecord!!.topic()}")
    }

    data class JobbData(val id: UUID)
}