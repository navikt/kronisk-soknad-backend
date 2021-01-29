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
import kotlin.reflect.KClass


class GenericMeldingKafkaProducer(props: Map<String, Any>, private val topicName: String, private val om : ObjectMapper) {
    private val producer = KafkaProducer(props, StringSerializer(), StringSerializer())

    fun <T> sendMessage(melding: T, type : String) : RecordMetadata? {
        val record: ProducerRecord<String, String> = ProducerRecord(topicName, om.writeValueAsString(melding))
        record.headers().add(RecordHeader("type", type.toByteArray()))
        return producer.send(record).get(10, TimeUnit.SECONDS)
    }
}
