package no.nav.helse.fritakagp.processing.brukernotifikasjon

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.GravidTestData
import no.nav.helse.KroniskTestData
import no.nav.helse.fritakagp.customObjectMapper
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helse.fritakagp.integration.kafka.BrukernotifikasjonSender
import no.nav.helse.fritakagp.processing.BakgrunnsJobbUtils
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor.Jobbdata.SkjemaType
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.builder.BuilderEnvironment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class BrukernotifikasjonProcessorTest {

    val ksRepo = mockk<KroniskSoeknadRepository>(relaxed = true)
    val kkRepo = mockk<KroniskKravRepository>(relaxed = true)
    val gkRepo = mockk<GravidKravRepository>(relaxed = true)
    val gsRepo = mockk<GravidSoeknadRepository>(relaxed = true)
    val kafkaSenderMock = mockk<BrukernotifikasjonSender>(relaxed = true)

    val objectMapper = customObjectMapper()

    val prosessor = BrukernotifikasjonProcessor(gkRepo, gsRepo, kkRepo, ksRepo, objectMapper, kafkaSenderMock, Sensitivitet.High)

    private var jobb = BakgrunnsJobbUtils.emptyJob()

    @BeforeEach
    fun setup() {
        mapOf(
            "NAIS_APP_NAME" to "test-app",
            "NAIS_NAMESPACE" to "test-namespace",
            "NAIS_CLUSTER_NAME" to "dev"
        ).let { naisEnv ->
            BuilderEnvironment.extend(naisEnv)
        }

        every { ksRepo.getById(any()) } returns KroniskTestData.soeknadKronisk
        every { kkRepo.getById(any()) } returns KroniskTestData.kroniskKrav
        every { gkRepo.getById(any()) } returns GravidTestData.gravidKrav
        every { gsRepo.getById(any()) } returns GravidTestData.soeknadGravid
    }

    @Test
    fun `skal sende kafkamelding med brukernotifikasjon for Kronisk Krav`() {
        jobb = BakgrunnsJobbUtils.testJob(
            objectMapper.writeValueAsString(
                BrukernotifikasjonProcessor.Jobbdata(UUID.randomUUID(), SkjemaType.KroniskKrav)
            )
        )
        prosessor.prosesser(jobb)

        verify(exactly = 1) { kkRepo.getById(any()) }
        verify(exactly = 1) { kafkaSenderMock.sendMessage(any(), any()) }
    }

    @Test
    fun `skal sende kafkamelding med brukernotifikasjon for Kronisk Søknad`() {
        jobb = BakgrunnsJobbUtils.testJob(
            objectMapper.writeValueAsString(
                BrukernotifikasjonProcessor.Jobbdata(UUID.randomUUID(), SkjemaType.KroniskSøknad)
            )
        )
        prosessor.prosesser(jobb)

        verify(exactly = 1) { ksRepo.getById(any()) }
        verify(exactly = 1) { kafkaSenderMock.sendMessage(any(), any()) }
    }

    @Test
    fun `skal sende kafkamelding med brukernotifikasjon for Gravid Krav`() {
        jobb = BakgrunnsJobbUtils.testJob(
            objectMapper.writeValueAsString(
                BrukernotifikasjonProcessor.Jobbdata(UUID.randomUUID(), SkjemaType.GravidKrav)
            )
        )
        prosessor.prosesser(jobb)

        verify(exactly = 1) { gkRepo.getById(any()) }
        verify(exactly = 1) { kafkaSenderMock.sendMessage(any(), any()) }
    }

    @Test
    fun `skal sende kafkamelding med brukernotifikasjon for Gravid Søknad`() {
        jobb = BakgrunnsJobbUtils.testJob(
            objectMapper.writeValueAsString(
                BrukernotifikasjonProcessor.Jobbdata(UUID.randomUUID(), SkjemaType.GravidSøknad)
            )
        )
        prosessor.prosesser(jobb)

        verify(exactly = 1) { gsRepo.getById(any()) }
        verify(exactly = 1) { kafkaSenderMock.sendMessage(any(), any()) }
    }
}
