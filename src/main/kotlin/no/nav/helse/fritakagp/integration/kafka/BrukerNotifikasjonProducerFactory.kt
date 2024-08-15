package no.nav.helse.fritakagp.integration.kafka

import no.nav.helsearbeidsgiver.utils.log.logger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

interface BrukernotifikasjonSender {
    fun sendMessage(varselId:String,varsel:String): RecordMetadata?
}

class BrukernotifikasjonKafkaProducer(
    props: Map<String, Any>,
    private val topicName: String
):BrukernotifikasjonSender{

    private var kafkaProducer: KafkaProducer<String, String> = KafkaProducer(props)
    private val logger = this.logger()
    override fun sendMessage(varselId:String,varsel:String): RecordMetadata? {

        return kafkaProducer.send(ProducerRecord(topicName, varselId, varsel)).get().also {
            logger.info("Skrevet vaselId $varselId til Kafka til topic ${it!!.topic()} med offset ${it.offset()}")
        }

    }
}
