package no.nav.helse.slowtests

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.fritakagp.db.SimpleJsonbEntity
import no.nav.helse.fritakagp.db.SimpleJsonbRepository
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.component.get
import java.time.LocalDateTime
import javax.sql.DataSource

sealed class RepositoryTest<T : SimpleJsonbEntity> : SystemTestBase() {
    private lateinit var repo: SimpleJsonbRepository<T>
    private lateinit var separateDataSource: DataSource

    abstract fun instantiateRepo(ds: DataSource, om: ObjectMapper): SimpleJsonbRepository<T>
    abstract fun T.alteredCopy(): T
    abstract fun mockEntity(
        opprettet: LocalDateTime = LocalDateTime.now(),
        virksomhetsnummer: String? = null,
    ): T
    abstract fun arrayOf(vararg elements: T): Array<T> // Omgår mangel på arrays med generisk innhold

    @BeforeEach
    fun setUp() {
        repo = instantiateRepo(
            createTestHikariDataSource(),
            get(),
        )

        separateDataSource = createTestHikariDataSource()
    }

    @Test
    fun `kan hente fra id`() {
        val toGet = mockEntity()
        val notGet = mockEntity()

        repo.withData(notGet) {
            repo.getById(toGet.id)
                .assert { it.isNull() }
        }

        repo.withData(toGet, notGet) {
            repo.getById(toGet.id)
                .assert { it.isEqualTo(toGet) }
                .assert { it.isNotEqualTo(notGet) }
        }
    }

    @Test
    fun `kan hente alle fra virksomhetsnummer`() {
        val toGet = arrayOf(
            mockEntity(),
            mockEntity(),
        )
        val notGet = mockEntity(virksomhetsnummer = "123")

        repo.withData(notGet) {
            repo.getAllByVirksomhet(toGet.first().virksomhetsnummer)
                .assert { it.isEmpty() }
        }

        repo.withData(*toGet, notGet) {
            repo.getAllByVirksomhet(toGet.first().virksomhetsnummer)
                .assert { it.containsExactlyInAnyOrder(*toGet) }
                .assert { it.doesNotContain(notGet) }
        }
    }

    @Test
    fun `kan legge til`() {
        val toInsert = mockEntity()
        val notInsert = mockEntity()

        repo.withData(notInsert) {
            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(notInsert) }

            repo.insert(toInsert)
                .assert { it.isEqualTo(1) }

            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(toInsert, notInsert) }

            repo.insert(toInsert)
                .assert { it.isEqualTo(0) }

            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(toInsert, notInsert) }
        }
    }

    @Test
    fun `kan legge til via spesifisert tilkobling`() {
        val toInsert = mockEntity()

        repo.getAll()
            .assert { it.isEmpty() }

        separateDataSource.connection.use { connection ->
            repo.insert(toInsert, connection)
                .assert { it.isEqualTo(1) }
        }

        repo.getAll()
            .assert { it.containsExactlyInAnyOrder(toInsert) }
    }

    @Test
    fun `kan slette`() {
        val toDelete = mockEntity()
        val notDelete = mockEntity()

        repo.withData(toDelete, notDelete) {
            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(toDelete, notDelete) }

            repo.delete(toDelete.id)
                .assert { it.isEqualTo(1) }

            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(notDelete) }

            repo.delete(toDelete.id)
                .assert { it.isEqualTo(0) }

            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(notDelete) }
        }
    }

    @Test
    fun `kan slette via spesifisert tilkobling`() {
        val toDelete = mockEntity()
        repo.withData(toDelete) {
            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(toDelete) }

            separateDataSource.connection.use { connection ->
                repo.delete(toDelete.id, connection)
                    .assert { it.isEqualTo(1) }
            }

            repo.getAll()
                .assert { it.isEmpty() }
        }
    }

    @Test
    fun `kan slette alle opprettet foer`() {
        val opprettetFoer = LocalDateTime.now()

        val toDelete = arrayOf(
            mockEntity(opprettet = opprettetFoer.minusDays(1)),
            mockEntity(opprettet = opprettetFoer.minusDays(2)),
        )
        val notDelete = mockEntity(opprettet = opprettetFoer.plusDays(1))

        repo.withData(*toDelete, notDelete) {
            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(*toDelete, notDelete) }

            repo.deleteAllOpprettetFoer(opprettetFoer)
                .assert { it.isEqualTo(toDelete.size) }

            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(notDelete) }

            repo.deleteAllOpprettetFoer(opprettetFoer)
                .assert { it.isEqualTo(0) }

            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(notDelete) }
        }
    }

    @Test
    fun `kan oppdatere`() {
        val beforeUpdate = mockEntity()
        val afterUpdate = beforeUpdate.alteredCopy()

        repo.withData(beforeUpdate) {
            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(beforeUpdate) }

            repo.update(afterUpdate)
                .assert { it.isEqualTo(1) }

            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(afterUpdate) }

            repo.update(afterUpdate)
                .assert { it.isEqualTo(1) }

            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(afterUpdate) }
        }
    }

    @Test
    fun `kan oppdatere via spesifisert tilkobling`() {
        val beforeUpdate = mockEntity()
        val afterUpdate = beforeUpdate.alteredCopy()

        repo.withData(beforeUpdate) {
            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(beforeUpdate) }

            separateDataSource.connection.use { connection ->
                repo.update(afterUpdate, connection)
                    .assert { it.isEqualTo(1) }
            }

            repo.getAll()
                .assert { it.containsExactlyInAnyOrder(afterUpdate) }
        }
    }

    @Test
    fun `kan hente alle (testfunksjon)`() {
        val mock1 = mockEntity()
        val mock2 = mockEntity().alteredCopy()
        val mock3 = mockEntity(
            opprettet = LocalDateTime.now().minusMonths(1),
            virksomhetsnummer = "123",
        )

        repo.getAll()
            .assert { it.isEmpty() }

        repo.insert(mock1)
            .assert { it.isEqualTo(1) }

        repo.getAll()
            .assert { it.containsExactlyInAnyOrder(mock1) }

        repo.insert(mock2)
            .assert { it.isEqualTo(1) }

        repo.getAll()
            .assert { it.containsExactlyInAnyOrder(mock1, mock2) }

        repo.insert(mock3)
            .assert { it.isEqualTo(1) }

        repo.getAll()
            .assert { it.containsExactlyInAnyOrder(mock1, mock2, mock3) }
    }
}

private fun <T : SimpleJsonbEntity> SimpleJsonbRepository<T>.withData(vararg entities: T, test: () -> Unit) {
    val inserted = entities.sumOf(::insert)
    Assertions.assertThat(inserted).isEqualTo(entities.size)

    test()

    entities.map { it.id }.forEach(::delete)
}
