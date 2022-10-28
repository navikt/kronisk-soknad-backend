package no.nav.helse.slowtests

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.fritakagp.db.ReferanseRepositoryImpl
import no.nav.helse.fritakagp.db.createTestHikariConfig
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ReferanseRepositoryTest : SystemTestBase() {

    lateinit var repo: ReferanseRepositoryImpl
    val testId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        val ds = HikariDataSource(createTestHikariConfig())
        repo = ReferanseRepositoryImpl(ds)
    }

    @AfterEach
    internal fun tearDown() {
        repo.delete(testId)
    }

    @Test
    fun `test getOrInsertReferanse`() {
        val result = repo.getOrInsertReferanse(testId)
        val result2 = repo.getOrInsertReferanse(testId)
        assertThat(result).isNotEqualTo(0)
        assertThat(result2).isEqualTo(result)
    }
}
