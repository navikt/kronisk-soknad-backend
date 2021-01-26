package no.nav.helse.fritakagp.integration.kafka

import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.fritakagp.ANTALL_INNKOMMENDE_MELDINGER
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

interface SoeknadsmeldingMeldingProvider {
    fun sendMessagesToProcess(melding: String)
}

interface SoeknadsmeldingMeldingConsumer {
    fun getMessagesToProcess(): List<String>
    fun confirmProcessingDone()
}

class SoeknadsmeldingKafkaProducer(props: MutableMap<String, Any>, topicName: String) :
        SoeknadsmeldingMeldingProvider,
        LivenessComponent {
    private var lastThrown: Exception? = null
    private  val topic = topicName
    private val producer = KafkaProducer(props, StringSerializer(), StringSerializer())

    override fun sendMessagesToProcess(melding: String) {
        producer.send(
            ProducerRecord(topic, melding)
        ).get(10, TimeUnit.SECONDS)

    }

    override suspend fun runLivenessCheck() {
        lastThrown?.let { throw lastThrown as Exception }
    }
}

class SoeknadsmeldingKafkaConsumer(props: MutableMap<String, Any>, topicName: String) :
    SoeknadsmeldingMeldingConsumer,
    LivenessComponent {
    private var currentBatch: List<String> = emptyList()
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, String> =
        KafkaConsumer(props, StringDeserializer(), StringDeserializer())
    private val  topicPartition = TopicPartition(topicName, 0)
    private  val topic = topicName

    private val log = LoggerFactory.getLogger(SoeknadsmeldingKafkaProducer::class.java)

    init {
        consumer.assign(Collections.singletonList(topicPartition))

        Runtime.getRuntime().addShutdownHook(Thread {
            log.debug("Got shutdown message, closing Kafka connection...")
            consumer.close()
            log.debug("Kafka connection closed")
        })
    }

    fun stop() = consumer.close()

    override fun getMessagesToProcess(): List<String> {
        if (currentBatch.isNotEmpty()) {
            return currentBatch
        }

        try {
            val kafkaMessages = consumer.poll(Duration.ofSeconds(10))
            val payloads = kafkaMessages.map {it.value() }
            lastThrown = null
            currentBatch = payloads

            log.debug("Fikk ${kafkaMessages.count()} meldinger med offsets ${kafkaMessages.map { it.offset() }.joinToString(", ")}")
            return payloads
        } catch (e: Exception) {
            lastThrown = e
            throw e
        }
    }

    override fun confirmProcessingDone() {
        consumer.commitSync()
        ANTALL_INNKOMMENDE_MELDINGER.inc(currentBatch.size.toDouble())
        currentBatch = emptyList()
    }

    override suspend fun runLivenessCheck() {
        lastThrown?.let { throw lastThrown as Exception }
    }
}

