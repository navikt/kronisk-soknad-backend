package no.nav.helse.slowtests.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.integration.kafka.SoeknadsmeldingKafkaProducer
import no.nav.helse.fritakagp.integration.kafka.consumerFakeConfig
import no.nav.helse.fritakagp.integration.kafka.producerFakeConfig
import no.nav.helse.slowtests.kafka.KafkaAdminForTests.Companion.topicName
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.test.inject


internal class SoeknadsmeldingKafkaProducerTest : SystemTestBase() {

    private val om by inject<ObjectMapper>()

    private lateinit var kafkaProdusent: KafkaAdminForTests
    private lateinit var producer: SoeknadsmeldingKafkaProducer
    private lateinit var consumer: SoeknadsmeldingKafkaConsumer

    @BeforeAll
    internal fun setUp() {

        kafkaProdusent = KafkaAdminForTests()
        producer = SoeknadsmeldingKafkaProducer(producerFakeConfig(), topicName, om = om)
        consumer = SoeknadsmeldingKafkaConsumer(consumerFakeConfig(), topicName)
    }

    @AfterAll
    internal fun tearDown() {
        kafkaProdusent.deleteTopicAndCloseConnection()
    }

    @Test
    fun getMessages() {

        val noMessagesExpected = consumer.getMessagesToProcess()

        assertThat(noMessagesExpected).isEmpty()

        kafkaProdusent.createTopicIfNotExists()
        producer.sendMessage(GravidTestData.soeknadGravid)
        val oneMessageExpected = consumer.getMessagesToProcess()
        assertThat(oneMessageExpected).hasSize(1)

        val stillSameMEssageExpected = consumer.getMessagesToProcess()
        assertThat(stillSameMEssageExpected).hasSize(1)
        assertThat(oneMessageExpected.first()).isEqualTo(stillSameMEssageExpected.first())

        consumer.confirmProcessingDone()

        val zeroMessagesExpected = consumer.getMessagesToProcess()
        assertThat(zeroMessagesExpected).isEmpty()

        consumer.stop()
    }
}