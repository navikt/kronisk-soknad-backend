package no.nav.helse.fritakagp.integration.kafka

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.helsearbeidsgiver.utils.log.logger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

interface BrukernotifikasjonBeskjedSender {
    fun sendMessage(nokkel: NokkelInput, beskjed: BeskjedInput): RecordMetadata?
}

class MockBrukernotifikasjonBeskjedSender : BrukernotifikasjonBeskjedSender {
    private val logger = this.logger()

    override fun sendMessage(nokkel: NokkelInput, beskjed: BeskjedInput): RecordMetadata? {
        logger.info("Sender Brukernotifikasjon: $beskjed")
        return null
    }
}

class BrukernotifikasjonBeskjedKafkaProducer(
    props: Map<String, Any>,
    private val topicName: String
) :
    BrukernotifikasjonBeskjedSender {
    private val logger = this.logger()
    private var producer: KafkaProducer<NokkelInput, BeskjedInput> = KafkaProducer(props)

    override fun sendMessage(nokkel: NokkelInput, beskjed: BeskjedInput): RecordMetadata? {
        val record: ProducerRecord<NokkelInput, BeskjedInput> = ProducerRecord(topicName, nokkel, beskjed)
        return producer.send(record).get().also {
            logger.info("Skrevet eventId ${nokkel.getEventId()} til Kafka til topic ${it!!.topic()} med offset ${it.offset()}")
        }
    }
}
