package no.nav.helse.fritakagp.integration.kafka

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory

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
    props: Map<String, Any>,
    private val topicName: String
) :
    BrukernotifikasjonBeskjedSender {
    val log = LoggerFactory.getLogger(BrukernotifikasjonBeskjedKafkaProducer::class.java)
    private var producer: KafkaProducer<NokkelInput, BeskjedInput> = KafkaProducer(props)

    override fun sendMessage(nokkel: NokkelInput, beskjed: BeskjedInput): RecordMetadata? {
        val record: ProducerRecord<NokkelInput, BeskjedInput> = ProducerRecord(topicName, nokkel, beskjed)
        return producer.send(record).get().also {
            log.info("Skrevet eventId ${nokkel.getEventId()} til Kafka til topic ${it!!.topic()} med offset ${it.offset()}")
        }
    }
}
