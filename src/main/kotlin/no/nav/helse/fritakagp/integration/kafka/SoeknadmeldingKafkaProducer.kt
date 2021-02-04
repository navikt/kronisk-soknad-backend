package no.nav.helse.fritakagp.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.utils.io.*
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.StringSerializer
import java.util.concurrent.TimeUnit
import org.apache.kafka.common.errors.AuthenticationException
import org.apache.kafka.common.errors.SaslAuthenticationException
import org.apache.kafka.common.errors.SslAuthenticationException
import java.util.concurrent.ExecutionException
import kotlin.math.expm1


interface SoeknadmeldingSender {
    fun sendMessage(melding: KroniskSoeknad): RecordMetadata?
    fun sendMessage(melding: GravidSoeknad): RecordMetadata?

}

class MockSoeknadmeldingKafkaProducer(
    private val props: MutableMap<String, Any>
) :
    SoeknadmeldingSender {
    override fun sendMessage(melding: KroniskSoeknad): RecordMetadata? {
        return sendKafkaMessage("melding", "KroniskSoeknad")
    }

    override fun sendMessage(melding: GravidSoeknad): RecordMetadata? {
        return sendKafkaMessage("melding", "GravidSoeknad")
    }

    private fun sendMelding(melding: String, type: String): RecordMetadata? {
        val saslValue = props["sasl.jaas.config"] as String
        if (!saslValue.contains("igroup"))
            throw AuthenticationException("feil brukernavn")

        return RecordMetadata(null,0,0,0,0,0,0)
    }

    private fun sendKafkaMessage(melding: String, type: String): RecordMetadata? {
        return try {
            sendMelding(melding, type)
        } catch (ex: ExecutionException) {
            throw ex
        }
    }
}

class SoeknadmeldingKafkaProducer(
    props: MutableMap<String, Any>,
    private val topicName: String,
    private val om: ObjectMapper
) :
    SoeknadmeldingSender {
    private var producer = KafkaProducer(props, StringSerializer(), StringSerializer())

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
                producer = KafkaProducer(producerLocalConfig(), StringSerializer(), StringSerializer())
                return sendMelding(melding, type)
            } else throw ex
        }
    }
}

