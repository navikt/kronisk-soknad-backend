package no.nav.helse.fritakagp.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.domain.KroniskKrav
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.errors.AuthenticationException
import org.apache.kafka.common.header.internals.RecordHeader
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

interface KravmeldingSender {
    fun sendMessage(melding: KroniskKrav): RecordMetadata?
    fun sendMessage(melding: GravidKrav): RecordMetadata?
}

class KravmeldingKafkaProducer(
    private val props: Map<String, Any>,
    private val topicName: String,
    private val om: ObjectMapper,
    private val producerFactory: ProducerFactory<String, String>
) :
    KravmeldingSender {
    private var producer = producerFactory.createProducer(props)

    override fun sendMessage(melding: KroniskKrav): RecordMetadata? {
        return sendKafkaMessage(om.writeValueAsString(melding), "KroniskKrav")
    }

    override fun sendMessage(melding: GravidKrav): RecordMetadata? {
        return sendKafkaMessage(om.writeValueAsString(melding), "GravidKrav")
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
                producer = producerFactory.createProducer(props)
                return sendMelding(melding, type)
            } else {
                throw ex
            }
        }
    }
}
