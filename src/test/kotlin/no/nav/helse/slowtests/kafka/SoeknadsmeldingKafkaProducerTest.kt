package no.nav.helse.slowtests.kafka

import kotlinx.coroutines.runBlocking
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.integration.kafka.SoeknadsmeldingKafkaConsumer
import no.nav.helse.fritakagp.integration.kafka.consumerFakeConfig
import no.nav.helse.slowtests.kafka.KafkaProducerForTests.Companion.topicName
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.core.get


internal class SoeknadsmeldingKafkaProducerTest : SystemTestBase() {
    private lateinit var kafkaProdusent: KafkaProducerForTests

    @BeforeAll
    internal fun setUp() {
        kafkaProdusent = KafkaProducerForTests(get())
    }

    @AfterAll
    internal fun tearDown() {
        kafkaProdusent.deleteTopicAndCloseConnection()
    }

    @Test
    internal fun testHealthCheck() {
        val client = SoeknadsmeldingKafkaConsumer(consumerFakeConfig(), topicName)

        runBlocking { client.runLivenessCheck() }

        client.stop()

        assertThatExceptionOfType(Exception::class.java).isThrownBy {
            runBlocking { client.getMessagesToProcess() }
        }

        assertThatExceptionOfType(Exception::class.java).isThrownBy {
            runBlocking { client.runLivenessCheck() }
        }
    }

    @Test
    fun getMessages() {

        val consumer = SoeknadsmeldingKafkaConsumer(consumerFakeConfig(), topicName)
        val noMessagesExpected = consumer.getMessagesToProcess()

        assertThat(noMessagesExpected).isEmpty()

        kafkaProdusent.sendSync(GravidTestData.fullValidRequest)

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