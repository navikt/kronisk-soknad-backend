package no.nav.helse.fritakagp.processing.gravid.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.integration.kafka.Producer
import no.nav.helse.fritakagp.integration.kafka.ProducerSaslConfig
import no.nav.helse.fritakagp.integration.kafka.ProducerType
import no.nav.helse.fritakagp.integration.kafka.SoeknadmeldingKafkaProducer
import no.nav.helse.slowtests.kafka.KafkaAdminForTests
import org.apache.kafka.clients.producer.RecordMetadata
import org.koin.test.KoinTest

class SoekandsmeldingKafkaProducerKotest: StringSpec() , KoinTest {
    private lateinit var kafkaProdusent: KafkaAdminForTests
    val om = ObjectMapper()
    private lateinit var producer : SoeknadmeldingKafkaProducer
    init {
        "feil egenskap" {
            om.registerModule(JavaTimeModule());
            om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            kafkaProdusent = KafkaAdminForTests()
            kafkaProdusent.createTopicIfNotExists()


            withEnvironment(mapOf("SASL_USERNAME" to "test", "SASL_PASSWORD" to "test")) {
                producer = SoeknadmeldingKafkaProducer(
                    ProducerSaslConfig() , KafkaAdminForTests.topicName, om = om, Producer(
                        ProducerType.PROD)
                )
                shouldThrow<Exception> {
                    producer.sendMessage(GravidTestData.soeknadGravid)
                }
            }

            withEnvironment(mapOf("SASL_USERNAME" to "igroup", "SASL_PASSWORD" to "itest")) {
                producer.sendMessage(GravidTestData.soeknadGravid).shouldBeInstanceOf<RecordMetadata>()
            }
            kafkaProdusent.deleteTopicAndCloseConnection()
        }
    }
}

