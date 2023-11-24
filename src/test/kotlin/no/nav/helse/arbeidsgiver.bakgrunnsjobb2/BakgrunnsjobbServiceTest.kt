package no.nav.helse.arbeidsgiver.bakgrunnsjobb2

import io.mockk.mockk
import kotlinx.coroutines.test.TestCoroutineScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException
import java.sql.Connection
import java.time.LocalDateTime

class BakgrunnsjobbServiceTest {

    val repoMock = MockBakgrunnsjobbRepository()
    val testCoroutineScope = TestCoroutineScope()
    val service = BakgrunnsjobbService(repoMock, 1, testCoroutineScope)

    val now = LocalDateTime.now()
    private val eksempelProsesserer = EksempelProsesserer()

    @BeforeEach
    internal fun setup() {
        service.registrer(eksempelProsesserer)
        repoMock.deleteAll()
        service.startAsync(true)
    }

    @Test
    fun `sett jobb til ok hvis ingen feil `() {
        val testJobb = Bakgrunnsjobb(
            type = EksempelProsesserer.JOBB_TYPE,
            data = "ok"
        )
        repoMock.save(testJobb)
        testCoroutineScope.testScheduler.apply { advanceTimeBy(1); runCurrent() }

        val resultSet = repoMock.findByKjoeretidBeforeAndStatusIn(LocalDateTime.now(), setOf(BakgrunnsjobbStatus.OK))
        assertThat(resultSet)
            .hasSize(1)

        val completeJob = resultSet[0]
        assertThat(completeJob.forsoek).isEqualTo(1)
    }

    @Test
    fun `sett jobb til stoppet og kjør stoppet-funksjonen hvis feiler for mye `() {
        val testJobb = Bakgrunnsjobb(
            type = EksempelProsesserer.JOBB_TYPE,
            opprettet = now.minusHours(1),
            maksAntallForsoek = 3,
            data = "fail"
        )
        repoMock.save(testJobb)
        testCoroutineScope.testScheduler.apply { advanceTimeBy(5); runCurrent() }

        // Den går rett til stoppet i denne testen
        assertThat(repoMock.findByKjoeretidBeforeAndStatusIn(now.plusMinutes(1), setOf(BakgrunnsjobbStatus.STOPPET)))
            .hasSize(1)

        assertThat(eksempelProsesserer.bleStoppet).isTrue()
    }

    @Test
    fun `autoClean opprettes feil parametre`() {
        var exception = Assertions.assertThrows(IllegalArgumentException::class.java) {
            service.startAutoClean(-1, 3)
        }
        Assertions.assertEquals("start autoclean må ha en frekvens støtte enn 1 og slettEldreEnnMaander større enn 0", exception.message)
        exception = Assertions.assertThrows(IllegalArgumentException::class.java) {
            service.startAutoClean(1, -1)
        }
        Assertions.assertEquals("start autoclean må ha en frekvens støtte enn 1 og slettEldreEnnMaander større enn 0", exception.message)
        assertThat(repoMock.findAutoCleanJobs()).hasSize(0)
    }

    @Test
    fun `autoClean opprettes med riktig kjøretid`() {
        service.startAutoClean(2, 3)
        assertThat(repoMock.findAutoCleanJobs()).hasSize(1)
        assert(
            repoMock.findAutoCleanJobs().get(0).kjoeretid > now.plusHours(1) &&
                repoMock.findAutoCleanJobs().get(0).kjoeretid < now.plusHours(3)
        )
    }

    @Test
    fun `autoClean oppretter jobb med riktig antall måneder`() {
        service.startAutoClean(2, 3)
        assertThat(repoMock.findAutoCleanJobs()).hasSize(1)
    }

    @Test
    fun `opprett lager korrekt jobb`() {
        val connectionMock = mockk<Connection>()
        service.opprettJobb<EksempelProsesserer>(data = "test", connection = connectionMock)
        val jobber =
            repoMock.findByKjoeretidBeforeAndStatusIn(LocalDateTime.MAX, setOf(BakgrunnsjobbStatus.OPPRETTET))
        assertThat(jobber).hasSize(1)
        assertThat(jobber[0].type).isEqualTo(EksempelProsesserer.JOBB_TYPE)
        assertThat(jobber[0].data).isEqualTo("test")
    }
}

class EksempelProsesserer : BakgrunnsjobbProsesserer {
    companion object {
        val JOBB_TYPE: String = "TEST_TYPE"
    }

    var bleStoppet: Boolean = false

    override val type = JOBB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        if (jobb.data == "fail") {
            throw RuntimeException()
        }
    }

    override fun stoppet(jobb: Bakgrunnsjobb) {
        bleStoppet = true
        throw RuntimeException()
    }

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return LocalDateTime.now()
    }
}
