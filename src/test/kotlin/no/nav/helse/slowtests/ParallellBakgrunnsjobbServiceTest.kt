package no.nav.helse.slowtests

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.KroniskTestData
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.BakgrunnsjobbStatus
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.MockBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.ParallellBakgrunnsjobbService
import no.nav.helse.fritakagp.customObjectMapper
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon.ArbeidsgiverOppdaterNotifikasjonProcessor
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.UUID

class ParallellBakgrunnsjobbServiceTest {

    @Test
    fun doJob() {
        val repo: BakgrunnsjobbRepository = MockBakgrunnsjobbRepository()
        val id = "98f60270-768e-48f5-890b-1229f076946c"
        val klient = mockk<ArbeidsgiverNotifikasjonKlient>(relaxed = true)
        val gravidRepo = mockk<GravidKravRepository>()
        val kroniskRepository = mockk<KroniskKravRepository>()
        every { kroniskRepository.getById(any()) } answers {
            Thread.sleep(10)
            KroniskTestData.kroniskKrav
        }

        every { gravidRepo.getById(any()) } throws SQLException()

        val service = ParallellBakgrunnsjobbService(repo, 1000)
        for (i in 1..1000) {
            repo.save(
                Bakgrunnsjobb(
                    uuid = UUID.randomUUID(),
                    type = "arbeidsgiveroppdaternotifikasjon",
                    data = """{"skjemaId": "$id", "skjemaType": "KroniskKrav"}"""
                )
            )
            if (i == 500) { // sleng inn en som feiler
                repo.save(
                    Bakgrunnsjobb(
                        uuid = UUID.randomUUID(),
                        type = "arbeidsgiveroppdaternotifikasjon",
                        data = """{"skjemaId": "$id", "skjemaType": "GravidKrav"}"""
                    )
                )
            }
        }

        service.registrer(ArbeidsgiverOppdaterNotifikasjonProcessor(gravidRepo, kroniskRepository, customObjectMapper(), klient))
        runBlocking {
            val job = coroutineScope {
                service.startAsync()
                delay(3000)
            }
        }
        coVerify(exactly = 1000) { klient.nyStatusSakByGrupperingsid(any(), any(), any(), any()) }
        verify(exactly = 1000) { kroniskRepository.getById(any()) }
        for (i in 1..1000) {
            repo.save(
                Bakgrunnsjobb(
                    uuid = UUID.randomUUID(),
                    type = "arbeidsgiveroppdaternotifikasjon",
                    data = """{"skjemaId": "$id", "skjemaType": "KroniskKrav"}"""
                )
            )
        }
        runBlocking {
            delay(5000)
        }
        coVerify(exactly = 2000) { klient.nyStatusSakByGrupperingsid(any(), any(), any(), any()) }
        verify(exactly = 2000) { kroniskRepository.getById(any()) }
        // kjøretid for jobb oppdateres til *neste* kjøringstidspunkt også når den er kjørt OK, så legger på en dag for å hente ut alle:
        assertEquals(2000, repo.findByKjoeretidBeforeAndStatusIn(LocalDateTime.now().plusDays(1), setOf(BakgrunnsjobbStatus.OK)).size)
        assertEquals(1, repo.findByKjoeretidBeforeAndStatusIn(LocalDateTime.now().plusDays(1), setOf(BakgrunnsjobbStatus.FEILET)).size)
        service.stop()
    }
}
