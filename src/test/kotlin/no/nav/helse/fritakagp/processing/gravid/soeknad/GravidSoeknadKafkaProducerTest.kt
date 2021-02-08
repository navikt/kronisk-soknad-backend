package no.nav.helse.fritakagp.processing.gravid.soeknad

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.integration.kafka.KafkaProducerProvider
import no.nav.helse.fritakagp.integration.kafka.SoeknadmeldingKafkaProducer
import no.nav.helse.fritakagp.integration.kafka.SoeknadmeldingKafkaProducerProvider
import no.nav.helse.fritakagp.integration.kafka.producerLocalSaslConfigWrongAuth
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.errors.AuthenticationException
import org.junit.jupiter.api.Test

class GravidSoeknadKafkaProducerTest {
    val omMock = mockk<ObjectMapper>()
    val producerProviderMock = mockk<KafkaProducerProvider>()
    val producerMock = mockk<KafkaProducer<String, String>>()

    @Test
    internal fun  `sdf`() {
        every { producerProviderMock.createProducer(any()) } returns producerMock
        val soeknadmelding = SoeknadmeldingKafkaProducer(producerLocalSaslConfigWrongAuth() as MutableMap<String, Any>, "test", omMock, producerProviderMock)
        soeknadmelding.sendMessage(GravidTestData.soeknadGravid)
        every { producerMock.send(any()) } throws AuthenticationException("feil bruker/passord")
        soeknadmelding.sendMessage(GravidTestData.soeknadGravid)
        verify { producerProviderMock.createProducer(any()) }
        //verify { producerMock.send(any()) }

    }
}