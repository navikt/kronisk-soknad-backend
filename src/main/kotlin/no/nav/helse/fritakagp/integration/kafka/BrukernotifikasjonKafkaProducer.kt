package no.nav.helse.fritakagp.integration.kafka

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.errors.AuthenticationException
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutionException


interface BrukernotifikasjonBeskjedSender {
    fun sendMessage(nokkel: Nokkel, beskjed: Beskjed): RecordMetadata?
}

class MockBrukernotifikasjonBeskjedSender : BrukernotifikasjonBeskjedSender {
    override fun sendMessage(nokkel: Nokkel, beskjed: Beskjed): RecordMetadata? {
        LoggerFactory.getLogger(this.javaClass).info("Sender Brukernotifikasjon: $beskjed")
        return null
    }
}

class BrukernotifikasjonBeskjedKafkaProducer(
    private val props: Map<String, Any>,
    private val topicName: String,
    private val producerFactory : ProducerFactory<Nokkel, Beskjed>
) :
    BrukernotifikasjonBeskjedSender {
    private var producer = producerFactory.createProducer(props)

    private fun sendMelding(nokkel: Nokkel, beskjed: Beskjed): RecordMetadata? {
        val record: ProducerRecord<Nokkel, Beskjed> = ProducerRecord(topicName, nokkel, beskjed)
        return producer.send(record).get()
    }

    private fun sendKafkaMessage(nokkel: Nokkel, beskjed: Beskjed): RecordMetadata? {
        return try {
            sendMelding(nokkel, beskjed)
        } catch (ex: ExecutionException) {
            if (ex.cause is AuthenticationException) {
                producer.flush()
                producer.close()
                producer = producerFactory.createProducer(props)
                return sendMelding(nokkel, beskjed)
            } else throw ex
        }

    }

    override fun sendMessage(nokkel: Nokkel, beskjed: Beskjed): RecordMetadata? {
        return sendKafkaMessage(nokkel, beskjed)
    }
}

