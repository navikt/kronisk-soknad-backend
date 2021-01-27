package no.nav.helse.fritakagp.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.StringSerializer
import java.util.concurrent.TimeUnit


interface SoeknadsmeldingMeldingProvider {
    fun sendMessage(melding: KroniskSoeknad): RecordMetadata?
    fun sendMessage(melding: GravidSoeknad): RecordMetadata?

}

class SoeknadsmeldingKafkaProducer(props: MutableMap<String, Any>, private val topicName: String, private val om : ObjectMapper) :
        SoeknadsmeldingMeldingProvider {
    private val producer = KafkaProducer(props, StringSerializer(), StringSerializer())

    override fun sendMessage(melding: KroniskSoeknad): RecordMetadata?{
        val record: ProducerRecord<String, String> =  ProducerRecord(topicName, om.writeValueAsString(melding))
        record.headers().add(RecordHeader("type", "KroniskSoeknad".toByteArray()))
        return producer.send(record).get(10, TimeUnit.SECONDS)
    }

    override fun sendMessage(melding: GravidSoeknad): RecordMetadata? {
        val record: ProducerRecord<String, String> =  ProducerRecord(topicName, om.writeValueAsString(melding))
        record.headers().add(RecordHeader("type", "GravidSoeknad".toByteArray()))
        return producer.send(record).get(10, TimeUnit.SECONDS)
    }
}

