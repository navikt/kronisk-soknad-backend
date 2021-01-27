package no.nav.helse.fritakagp.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.domain.KroniskKrav
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
    fun sendMessage(melding: KroniskKrav): RecordMetadata?
    fun sendMessage(melding: GravidKrav): RecordMetadata?

}

class SoeknadsmeldingKafkaProducer(props: MutableMap<String, Any>, private val topicName: String, private val om : ObjectMapper) :
        SoeknadsmeldingMeldingProvider {
    private val producer = KafkaProducer(props, StringSerializer(), StringSerializer())

    override fun sendMessage(melding: KroniskSoeknad): RecordMetadata?{
        return sendKafkaMessage(om.writeValueAsString(melding), "KroniskSoeknad")
    }

    override fun sendMessage(melding: GravidSoeknad): RecordMetadata? {
        return sendKafkaMessage(om.writeValueAsString(melding), "GravidSoeknad")
    }

    override fun sendMessage(melding: GravidKrav): RecordMetadata? {
        return sendKafkaMessage(om.writeValueAsString(melding), "GravidKrav")
    }

    override fun sendMessage(melding: KroniskKrav): RecordMetadata? {
        return sendKafkaMessage(om.writeValueAsString(melding), "KroniskKrav")
    }

    private fun sendKafkaMessage(melding: String, type : String): RecordMetadata? {
        val record: ProducerRecord<String, String> = ProducerRecord(topicName, melding)
        record.headers().add(RecordHeader("type", type.toByteArray()))
        return producer.send(record).get(10, TimeUnit.SECONDS)
    }
}

