package no.nav.helse.fritakagp.processing.gravid.soeknad

import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.integration.kafka.SoeknadsmeldingKafkaProducer
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class GravidSoeknadKafkaProcessor(
    private val gravidSoeknad: GravidSoeknad,
    private val kafkaProducer: SoeknadsmeldingKafkaProducer
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "PROC_GRAVID_KAFKA"
    }

    val log = LoggerFactory.getLogger(GravidSoeknadKafkaProcessor::class.java)

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return LocalDateTime.now().plusHours(3)
    }

    /**
     * Sender gravidsoeknad til Kafka kø
     */
    override fun prosesser(jobbDataString: String) {
        val retRecord = kafkaProducer.sendMessage(gravidSoeknad)
        log.info("Skrevet ${gravidSoeknad.id} til Kafka til topic ${retRecord!!.topic()}")
    }

    data class JobbData(val id: UUID)
}