package no.nav.helse.slowtests.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.integration.kafka.SoeknadmeldingKafkaProducer
import no.nav.helse.fritakagp.integration.kafka.StringKafkaProducerFactory
import no.nav.helse.fritakagp.integration.kafka.consumerFakeConfig
import no.nav.helse.fritakagp.integration.kafka.localCommonKafkaProps
import no.nav.helse.slowtests.kafka.KafkaAdminForTests.Companion.topicName
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.test.inject
import kotlin.test.assertEquals

internal class SoeknadsmeldingKafkaProducerTest : SystemTestBase() {

    private val om by inject<ObjectMapper>()

    private lateinit var kafkaProdusent: KafkaAdminForTests

    @BeforeAll
    internal fun setUp() {
        kafkaProdusent = KafkaAdminForTests()
    }

    @AfterAll
    internal fun tearDown() {
        kafkaProdusent.deleteTopicAndCloseConnection()
    }

    @Test
    fun getMessages() {
        val consumer = SoeknadsmeldingKafkaConsumer(consumerFakeConfig(), topicName)
        val noMessagesExpected = consumer.getMessagesToProcess()
        val producer = SoeknadmeldingKafkaProducer(localCommonKafkaProps(), topicName, om = om, StringKafkaProducerFactory())
        assertThat(noMessagesExpected).isEmpty()

        kafkaProdusent.createTopicIfNotExists()
        producer.sendMessage(GravidTestData.soeknadGravid)
        val oneMessageExpected = consumer.getMessagesToProcess()
        assertThat(oneMessageExpected).hasSize(1)

        val stillSameMEssageExpected = consumer.getMessagesToProcess()
        assertThat(stillSameMEssageExpected).hasSize(1)

        assertEquals(oneMessageExpected.first(), stillSameMEssageExpected.first(), "is Equal")
        consumer.confirmProcessingDone()

        val zeroMessagesExpected = consumer.getMessagesToProcess()
        assertThat(zeroMessagesExpected).isEmpty()

        consumer.stop()
    }
}
