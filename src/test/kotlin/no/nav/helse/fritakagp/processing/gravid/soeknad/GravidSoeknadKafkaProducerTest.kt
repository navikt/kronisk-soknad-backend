package no.nav.helse.fritakagp.processing.gravid.soeknad

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.integration.kafka.KafkaProducerProvider
import no.nav.helse.fritakagp.integration.kafka.SoeknadmeldingKafkaProducer
import no.nav.helse.fritakagp.integration.kafka.SoeknadmeldingKafkaProducerProvider
import no.nav.helse.fritakagp.integration.kafka.producerLocalSaslConfigWrongAuth
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.errors.AuthenticationException
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

class GravidSoeknadKafkaProducerTest {
    val omMock = ObjectMapper().registerModules(KotlinModule(),JavaTimeModule())
    val producerProviderMock = mockk<KafkaProducerProvider>()
    val producerMock = mockk<KafkaProducer<String, String>>()

    @BeforeEach
    fun setUp() {



    }
    @Test
    internal fun  `sdf`() {
        every { producerProviderMock.createProducer(any()) } returns producerMock
        every { producerMock.send(any()) } throws AuthenticationException("feil bruker/passord")
        val soeknadmelding = SoeknadmeldingKafkaProducer(producerLocalSaslConfigWrongAuth() as MutableMap<String, Any>, "test", omMock, producerProviderMock)
        try {
            soeknadmelding.sendMessage(GravidTestData.soeknadGravid)
            Assert.fail("feil bruker/passord")
        } catch (ex: ExecutionException) {
            print(ex.message)
        }
        every { producerMock.send(any()).get(10, TimeUnit.SECONDS) } returns RecordMetadata(null,0,0,0,0,0,0)
        soeknadmelding.sendMessage(GravidTestData.soeknadGravid)
        verify(exactly = 2) { producerProviderMock.createProducer(any()) }
        //verify { producerMock.send(any()) }

    }
}