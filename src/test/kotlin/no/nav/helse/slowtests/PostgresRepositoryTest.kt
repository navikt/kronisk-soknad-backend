package no.nav.helse.slowtests

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.fritakagp.db.PostgresRepository
import no.nav.helse.fritakagp.db.createTestHikariConfig
import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.domain.SoeknadGravid
import no.nav.helse.fritakagp.domain.Tiltak
import no.nav.helse.fritakagp.koin.common
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get
import java.time.LocalDate

class PostgresRepositoryTest : KoinComponent {

    lateinit var repo: PostgresRepository
    val testSoeknad = SoeknadGravid(
            dato = LocalDate.now(),
            fnr = "12345",
            sendtAv = "12345678911",
            omplassering = "Ja",
            omplasseringAarsak = null,
            tilrettelegge = true,
            tiltak = listOf(Tiltak.ANNET),
            tiltakBeskrivelse = "tiltakBeskrivelse",
            datafil = null,
            ext = null
    )

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)

        }
        repo = PostgresRepository(HikariDataSource(createTestHikariConfig()), get())
        repo.insert(testSoeknad)

    }

    @AfterEach
    internal fun tearDown() {
       repo.delete(testSoeknad.id)
        stopKoin()
    }

    @Test
    fun `finner data i db`() {
        val soeknadGravidResult = repo.getById(testSoeknad.id)
        assertThat(soeknadGravidResult).isEqualTo(testSoeknad)
    }

}
