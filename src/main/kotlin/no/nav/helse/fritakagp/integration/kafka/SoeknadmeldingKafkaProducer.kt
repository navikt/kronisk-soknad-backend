package no.nav.helse.fritakagp.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.StringSerializer
import java.util.concurrent.TimeUnit
import org.apache.kafka.common.errors.AuthenticationException
import java.util.concurrent.ExecutionException


interface SoeknadmeldingSender {
    fun sendMessage(melding: KroniskSoeknad): RecordMetadata?
    fun sendMessage(melding: GravidSoeknad): RecordMetadata?
}

interface KafkaProducerProvider {
    fun createProducer(props : Map<String, Any>) : KafkaProducer<String, String>
}

class SoeknadmeldingKafkaProducerProvider : KafkaProducerProvider {
    override fun createProducer(props: Map<String, Any>) = KafkaProducer(props, StringSerializer(), StringSerializer())
}

class SoeknadmeldingKafkaProducer(
    private val props: MutableMap<String, Any>,
    private val topicName: String,
    private val om: ObjectMapper,
    private val producerProvider : KafkaProducerProvider
) :
    SoeknadmeldingSender {
    private var producer = producerProvider.createProducer(props)



    override fun sendMessage(melding: KroniskSoeknad): RecordMetadata? {
        return sendKafkaMessage(om.writeValueAsString(melding), "KroniskSoeknad")
    }

    override fun sendMessage(melding: GravidSoeknad): RecordMetadata? {
        return sendKafkaMessage(om.writeValueAsString(melding), "GravidSoeknad")
    }

    private fun sendMelding(melding: String, type: String): RecordMetadata? {
        val record: ProducerRecord<String, String> = ProducerRecord(topicName, melding)
        record.headers().add(RecordHeader("type", type.toByteArray()))
        return producer.send(record).get(10, TimeUnit.SECONDS)
    }

    private fun sendKafkaMessage(melding: String, type: String): RecordMetadata? {
        return try {
            sendMelding(melding, type)
        } catch (ex: ExecutionException) {
            if (ex.cause is AuthenticationException) {
                producer.flush()
                producer.close()
                producer = producerProvider.createProducer(props)
                return sendMelding(melding, type)
            } else throw ex
        }
    }
}