package no.nav.helse.slowtests

    import com.zaxxer.hikari.HikariDataSource
    import no.nav.helse.TestData
    import no.nav.helse.fritakagp.db.PostgresKvitteringRepository
    import no.nav.helse.fritakagp.db.createTestHikariConfig
    import no.nav.helse.fritakagp.koin.common
    import no.nav.helse.fritakagp.processing.kvittering.Kvittering
    import no.nav.helse.fritakagp.processing.kvittering.KvitteringStatus
    import no.nav.helse.slowtests.systemtests.api.SystemTestBase
    import org.assertj.core.api.Assertions.assertThat
    import org.flywaydb.core.Flyway
    import org.junit.jupiter.api.AfterEach
    import org.junit.jupiter.api.BeforeEach
    import org.junit.jupiter.api.Test
    import org.koin.core.KoinComponent
    import org.koin.core.context.loadKoinModules
    import org.koin.core.context.startKoin
    import org.koin.core.context.stopKoin
    import org.koin.core.get
    import java.time.LocalDate
    import java.time.LocalDateTime

    internal class PostgresKvitteringRepositoryTest : SystemTestBase() {

        lateinit var repo: PostgresKvitteringRepository
        lateinit var kvittering: Kvittering

        @BeforeEach
        internal fun setUp() {
            val ds = HikariDataSource(createTestHikariConfig())

            repo = PostgresKvitteringRepository(ds, get())
            kvittering = repo.insert(Kvittering(
                    tidspunkt = LocalDateTime.now(), id = TestData.soeknadGravid.id, virksomhetsnummer = TestData.soeknadGravid.orgnr.toString(), status = KvitteringStatus.OPPRETTET))
        }

        @AfterEach
        internal fun tearDown() {
            repo.delete(kvittering.id)
        }


        @Test
        fun `kanHenteLagretKvittering`() {
            val rs = repo.getById(kvittering.id)

            assertThat(rs).isNotNull
            assertThat(rs).isEqualTo(kvittering)
        }


        @Test
        fun `kanHenteFraStatus`() {
            val kvitteringListe = repo.getByStatus(KvitteringStatus.OPPRETTET, 10)
            assertThat(kvitteringListe.size).isEqualTo(1)
            assertThat(kvitteringListe.first()).isEqualTo(kvittering)
        }

        @Test
        fun `kanOppdatereKrav`() {
            val kvitteringListe = repo.getByStatus(KvitteringStatus.OPPRETTET, 10)
            val kvittering = kvitteringListe.first()


            kvittering.status = KvitteringStatus.SENDT

            repo.update(kvittering)

            val fromDb = repo.getById(kvittering.id)

            assertThat(kvittering).isEqualTo(fromDb)
        }

        @Test
        fun `kanSletteEtRefusjonskrav`() {
            val deletedCount = repo.delete(kvittering.id)
            assertThat(deletedCount).isEqualTo(1)
        }
    }

