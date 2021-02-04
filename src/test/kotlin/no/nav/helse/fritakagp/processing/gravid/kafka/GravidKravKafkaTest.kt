package no.nav.helse.fritakagp.processing.gravid.kafka

import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.integration.kafka.MockSoeknadmeldingKafkaProducer
import no.nav.helse.fritakagp.integration.kafka.producerLocalSaslConfig
import no.nav.helse.fritakagp.integration.kafka.producerLocalSaslConfigWrongAuth
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.errors.AuthenticationException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GravidKravKafkaTest {

    @Test
    fun `funksjonen oppretter en feil hvis egenskapene ikke inneholder riktig brukernavn`() {
        val mockProducer = MockSoeknadmeldingKafkaProducer(producerLocalSaslConfigWrongAuth())
        Assertions.assertThrows(
            AuthenticationException::class.java,
            { mockProducer.sendMessage(GravidTestData.soeknadGravid)}
            , "feil brukernavn")
    }

    @Test
    fun `funksjonen retunere RecordMetaData hvis egenskapene inneholder riktig brukernavn`() {
        val mockProducer = MockSoeknadmeldingKafkaProducer(producerLocalSaslConfig())
        val ret = mockProducer.sendMessage(GravidTestData.soeknadGravid)

        Assertions.assertEquals(ret!!::class.java, RecordMetadata::class.java)
    }
}