package no.nav.helse.slowtests.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.fritakagp.integration.kafka.SoeknadsmeldingKafkaProducer
import no.nav.helse.fritakagp.integration.kafka.consumerFakeConfig
import no.nav.helse.fritakagp.integration.kafka.producerFakeConfig
import no.nav.helse.fritakagp.web.api.resreq.GravidSoknadRequest
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.errors.TopicExistsException
import java.util.concurrent.TimeUnit

class KafkaProducerForTests(private val om: ObjectMapper) {
    companion object {
        const val topicName = "manglende-inntektsmelding-test"
    }

    private val adminClient: AdminClient = KafkaAdminClient.create(consumerFakeConfig())
    private val producer = SoeknadsmeldingKafkaProducer(producerFakeConfig(), topicName)

    fun sendSync(melding: GravidSoknadRequest) {
        createTopicIfNotExists()
        producer.sendMessagesToProcess(om.writeValueAsString(melding))
    }

    private fun createTopicIfNotExists() {
        try {
            adminClient
                    .createTopics(mutableListOf(NewTopic(topicName, 1, 1)))
                    .all()
                    .get(30, TimeUnit.SECONDS)
        } catch(createException: java.util.concurrent.ExecutionException) {
            if (createException.cause is TopicExistsException) {
                println("topic exists")
            } else {
                throw createException
            }
        }
    }

    fun deleteTopicAndCloseConnection() {
        try {
            adminClient
                    .deleteTopics(mutableListOf(topicName))
                    .all()
                    .get(30, TimeUnit.SECONDS)
        } catch (ex: Exception) {
            println("can't delete topic")
        }
        adminClient.close()
    }
}