package no.nav.helse.fritakagp.processing.gravid.krav

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.integration.kafka.KravmeldingSender
import org.slf4j.LoggerFactory
import java.util.UUID

class GravidKravKafkaProcessor(
    private val gravidKravRepo: GravidKravRepository,
    private val kafkaProducer: KravmeldingSender,
    private val om: ObjectMapper
) : BakgrunnsjobbProsesserer {
    val log = LoggerFactory.getLogger(GravidKravKafkaProcessor::class.java)
    companion object { val JOB_TYPE = "gravid-krav-send-kafka" }
    override val type: String get() = JOB_TYPE

    /**
     * Sender gravidkrav til Kafka kø
     */
    override fun prosesser(jobb: Bakgrunnsjobb) {
        val jobbData = om.readValue<JobbData>(jobb.data)
        val gravidKrav = gravidKravRepo.getById(jobbData.id) ?: throw java.lang.IllegalStateException("${jobbData.id} fantes ikke")
        val retRecord = kafkaProducer.sendMessage(gravidKrav)
        log.info("Skrevet ${gravidKrav.id} til Kafka til topic ${retRecord!!.topic()}")
    }

    data class JobbData(val id: UUID)
}
