package no.nav.helse.fritakagp.integration.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.pdfbox.pdmodel.interactive.annotation.layout.PlainText

private const val JAVA_KEYSTORE = "jks"
private const val PKCS12 = "PKCS12"
private const val LOCALHOST = "localhost:9092"
private const val GROUP_ID_CONFIG = "helsearbeidsgiver-im-varsel-grace-period"
private const val SASL_MECHANISM = "PLAIN"

private fun envOrThrow(envVar: String) = System.getenv()[envVar] ?: throw IllegalStateException("$envVar er påkrevd miljøvariabel")

fun producerConfig() = mutableMapOf<String, Any>(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to envOrThrow("KAFKA_BROKERS"),
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.ACKS_CONFIG to "all",

    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SSL.name,
    SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "", //Disable server host name verification
    SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to JAVA_KEYSTORE,
    SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to PKCS12,
    SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to envOrThrow("KAFKA_TRUSTSTORE_PATH"),
    SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to envOrThrow("KAFKA_CREDSTORE_PASSWORD"),
    SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to envOrThrow("KAFKA_KEYSTORE_PATH"),
    SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to envOrThrow("KAFKA_CREDSTORE_PASSWORD"),
    SslConfigs.SSL_KEY_PASSWORD_CONFIG to envOrThrow("KAFKA_CREDSTORE_PASSWORD")
)

fun producerLocalConfig() = mutableMapOf<String, Any>(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to LOCALHOST,
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.ACKS_CONFIG to "1"
)

fun producerLocalSaslConfig() = mutableMapOf<String, Any>(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to LOCALHOST,
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.ACKS_CONFIG to "1"
) + saslConfig()


fun producerLocalSaslConfigWrongAuth() = mutableMapOf<String, Any>(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to LOCALHOST,
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.ACKS_CONFIG to "1"
) + saslConfigFeilPassord()

fun consumerFakeConfig() = mutableMapOf<String, Any>(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to LOCALHOST,
        ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "30000",
        ConsumerConfig.GROUP_ID_CONFIG to GROUP_ID_CONFIG,
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest"
    )

fun consumerFakeSaslConfig() = mutableMapOf<String, Any>(
    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to LOCALHOST,
    ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "30000",
    ConsumerConfig.GROUP_ID_CONFIG to GROUP_ID_CONFIG,
    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest"
) + saslConfig()

fun saslConfig()  = mutableMapOf<String, Any>(
    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SASL_PLAINTEXT.name,
    SaslConfigs.SASL_MECHANISM to SASL_MECHANISM,
    SaslConfigs.SASL_JAAS_CONFIG to lagreJassTemplate("igroup", "itest")
)

fun saslConfigFeilPassord()  = mutableMapOf<String, Any>(
    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SASL_PLAINTEXT.name,
    SaslConfigs.SASL_MECHANISM to SASL_MECHANISM,
    SaslConfigs.SASL_JAAS_CONFIG to lagreJassTemplate("admin", "admin")
)

fun lagreJassTemplate(usernavn: String, passord: String) : String{
    return "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"$usernavn\" password=\"$passord\";";
}

fun ProducerSaslConfig() = mutableMapOf<String, Any>(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to LOCALHOST,
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.ACKS_CONFIG to "1",
    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SASL_PLAINTEXT.name,
    SaslConfigs.SASL_MECHANISM to SASL_MECHANISM,
    SaslConfigs.SASL_JAAS_CONFIG to lagreJassTemplate(envOrThrow("SASL_USERNAME"), envOrThrow("SASL_PASSWORD"))
)
