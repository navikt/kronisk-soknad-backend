package no.nav.helse.fritakagp.integration.kafka

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.errors.AuthenticationException
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutionException

interface BrukernotifikasjonBeskjedSender {
    fun sendMessage(nokkel: NokkelInput, beskjed: BeskjedInput): RecordMetadata?
}

class MockBrukernotifikasjonBeskjedSender : BrukernotifikasjonBeskjedSender {

    override fun sendMessage(nokkel: NokkelInput, beskjed: BeskjedInput): RecordMetadata? {
        LoggerFactory.getLogger(this.javaClass).info("Sender Brukernotifikasjon: $beskjed")
        return null
    }
}

class BrukernotifikasjonBeskjedKafkaProducer(
    private val props: Map<String, Any>,
    private val topicName: String,
    private val producerFactory: ProducerFactory<NokkelInput, BeskjedInput>
) :
    BrukernotifikasjonBeskjedSender {
    val log = LoggerFactory.getLogger(BrukernotifikasjonBeskjedKafkaProducer::class.java)
    private var producer = producerFactory.createProducer(props)

    private fun sendMelding(nokkel: NokkelInput, beskjed: BeskjedInput): RecordMetadata? {
        val record: ProducerRecord<NokkelInput, BeskjedInput> = ProducerRecord(topicName, nokkel, beskjed)
        return producer.send(record).get()
    }

    private fun sendKafkaMessage(nokkel: NokkelInput, beskjed: BeskjedInput): RecordMetadata? {
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

    override fun sendMessage(nokkel: NokkelInput, beskjed: BeskjedInput): RecordMetadata? {
        val retrievedRecord = sendKafkaMessage(nokkel, beskjed)
        log.info("Skrevet nøkkel: $nokkel beskjed: $beskjed til Kafka til topic ${retrievedRecord!!.topic()} offset: ${retrievedRecord.offset()}")
        return retrievedRecord
    }
}
