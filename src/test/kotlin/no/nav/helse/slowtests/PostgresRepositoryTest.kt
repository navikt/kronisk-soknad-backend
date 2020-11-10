package no.nav.helse.slowtests

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.fritakagp.db.PostgresRepository
import no.nav.helse.fritakagp.db.createTestHikariConfig
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

class PostgresRepositoryTest : KoinComponent {

    lateinit var repo: PostgresRepository
    val testString = "TestString"

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)

        }
        repo = PostgresRepository(HikariDataSource(createTestHikariConfig()), get())
        repo.insert(testString, 10)

    }

    @AfterEach
    internal fun tearDown() {
        repo.delete(10)
        stopKoin()
    }


    @Test
    fun `finner data i db`() {
        val resultString = repo.getById(10)
        assertThat(resultString).isEqualTo(testString)
    }

}
