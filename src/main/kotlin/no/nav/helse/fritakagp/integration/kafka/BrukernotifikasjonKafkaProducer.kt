package no.nav.helse.fritakagp.integration.kafka

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory

interface BrukernotifikasjonBeskjedSender {
    fun sendMessage(nokkel: NokkelInput, beskjed: BeskjedInput): RecordMetadata?
    fun flush()
}

class MockBrukernotifikasjonBeskjedSender : BrukernotifikasjonBeskjedSender {

    override fun sendMessage(nokkel: NokkelInput, beskjed: BeskjedInput): RecordMetadata? {
        LoggerFactory.getLogger(this.javaClass).info("Sender Brukernotifikasjon: $beskjed")
        return null
    }

    override fun flush() {
        TODO("Not yet implemented")
    }
}

class BrukernotifikasjonBeskjedKafkaProducer(
    props: Map<String, Any>,
    private val topicName: String,
    producerFactory: ProducerFactory<NokkelInput, BeskjedInput>
) :
    BrukernotifikasjonBeskjedSender {
    val log = LoggerFactory.getLogger(BrukernotifikasjonBeskjedKafkaProducer::class.java)
    private var producer = producerFactory.createProducer(props)

    override fun flush() {
        log.info("Flusher")
        producer.flush()
    }

    override fun sendMessage(nokkel: NokkelInput, beskjed: BeskjedInput): RecordMetadata? {
        val record: ProducerRecord<NokkelInput, BeskjedInput> = ProducerRecord(topicName, nokkel, beskjed)
        return producer.send(record).get().also {
            log.info("Sender melding med nøkkel $nokkel og beskjed $beskjed. Fikk offset ${it.offset()} ${it.serializedKeySize()} ${it.serializedValueSize()} ${it.topic()} ${it.timestamp()}")
        }
    }
}
