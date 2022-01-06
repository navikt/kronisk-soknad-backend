package no.nav.helse.fritakagp.processing.gravid.soeknad

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.integration.kafka.ProducerFactory
import no.nav.helse.fritakagp.integration.kafka.SoeknadmeldingKafkaProducer
import no.nav.helse.fritakagp.integration.kafka.localCommonKafkaProps
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.errors.AuthenticationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class GravidSoeknadKafkaProducerTest {
    val omMock = ObjectMapper().registerModules(KotlinModule(), JavaTimeModule())
    val producerProviderMock = mockk<ProducerFactory<String, String>>()
    val unauthorizedProducerMock = mockk<KafkaProducer<String, String>>(relaxed = true)
    val authedProducerMock = mockk<KafkaProducer<String, String>>(relaxed = true)

    @BeforeEach
    fun setUp() {
        every { authedProducerMock.send(any()).get(10, TimeUnit.SECONDS) } returns RecordMetadata(null, 0, 0, 0, 0, 0, 0)
        every { unauthorizedProducerMock.send(any()) } throws ExecutionException(AuthenticationException("feil bruker/passord"))
    }

    @Test
    internal fun `Failure to rotate password throws execption after trying again once`() {
        every { producerProviderMock.createProducer(any()) } returns unauthorizedProducerMock
        val soeknadmelding = SoeknadmeldingKafkaProducer(localCommonKafkaProps(), "test", omMock, producerProviderMock)

        assertThrows<ExecutionException> {
            soeknadmelding.sendMessage(GravidTestData.soeknadGravid)
        }

        verify(exactly = 2) { producerProviderMock.createProducer(any()) }
        verify(exactly = 2) { unauthorizedProducerMock.send(any()) }
        verify(exactly = 1) { unauthorizedProducerMock.close() }
    }

    @Test
    internal fun `Success to rotate password returns record with no error`() {
        every { producerProviderMock.createProducer(any()) } returns unauthorizedProducerMock
        val soeknadmelding = SoeknadmeldingKafkaProducer(localCommonKafkaProps(), "test", omMock, producerProviderMock)
        every { producerProviderMock.createProducer(any()) } returns authedProducerMock

        soeknadmelding.sendMessage(GravidTestData.soeknadGravid)

        verify(exactly = 2) { producerProviderMock.createProducer(any()) }
        verify(exactly = 1) { unauthorizedProducerMock.send(any()) }
        verify(exactly = 1) { unauthorizedProducerMock.close() }
        verify(exactly = 1) { authedProducerMock.send(any()) }
    }
}
