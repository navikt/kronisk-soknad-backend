package no.nav.helse.fritakagp.integration.kafka

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer

interface ProducerFactory<K, V> {
    fun createProducer(props: Map<String, Any>): Producer<K, V>
}

class StringKafkaProducerFactory : ProducerFactory<String, String> {
    override fun createProducer(props: Map<String, Any>) = KafkaProducer(
        props + mapOf(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName
        ),
        StringSerializer(),
        StringSerializer()
    )
}

class BeskjedInputProducerFactory(val kafkaSchemaRegistryUrl: String) : ProducerFactory<NokkelInput, BeskjedInput> {
    override fun createProducer(props: Map<String, Any>) = KafkaProducer<NokkelInput, BeskjedInput>(
        props + mapOf(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
            AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to kafkaSchemaRegistryUrl
        )
    )
}
