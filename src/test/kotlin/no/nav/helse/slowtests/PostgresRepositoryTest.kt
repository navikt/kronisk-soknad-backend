package no.nav.helse.slowtests

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.fritakagp.db.PostgresRepository
import no.nav.helse.fritakagp.db.createHikariConfig
import no.nav.helse.fritakagp.web.common
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get

class PostgresRepositoryTest : KoinComponent {

    lateinit var repo: PostgresRepository
    lateinit var testString: String

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)

        }
        repo = PostgresRepository(get(), get())
        testString = repo.insert("Privyet mir", 4)
    }

    @AfterEach
    internal fun tearDown() {
        repo.delete(4)
        stopKoin()
    }

    @Test
    fun `Finner teststrengen`() {
        assertThat(repo.getById(4)).isEqualTo("Privyet mir")
    }

}
