package no.nav.helse.slowtests

import no.nav.helse.TestData
import no.nav.helse.fritakagp.db.PostgresGravidSoeknadRepository
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.get
import kotlin.test.assertNotNull

class PostgresGravidSoeknadRepositoryTest : SystemTestBase() {

    lateinit var repo: PostgresGravidSoeknadRepository
    val testSoeknad = TestData.soeknadGravid

    @BeforeEach
    internal fun setUp() {
        repo = PostgresGravidSoeknadRepository(get(), get())
        repo.insert(testSoeknad)
    }

    @AfterEach
    internal fun tearDown() {
       repo.delete(testSoeknad.id)
    }

    @Test
    fun `finner data i db`() {
        val soeknadGravidResult = repo.getById(testSoeknad.id)
        assertThat(soeknadGravidResult).isEqualTo(testSoeknad)
    }

    @Test
    fun `kan oppdatere data`() {
        val soeknadGravidResult = repo.getById(testSoeknad.id)
        assertNotNull(soeknadGravidResult, "Må finnes")

        soeknadGravidResult.journalpostId = "1234"
        soeknadGravidResult.oppgaveId = "78990"

        repo.update(soeknadGravidResult)

        val afterUpdate = repo.getById(soeknadGravidResult.id)
        assertThat(afterUpdate).isEqualTo(soeknadGravidResult)
    }

}
