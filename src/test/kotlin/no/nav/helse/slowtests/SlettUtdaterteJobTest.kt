package no.nav.helse.slowtests

import no.nav.helse.fritakagp.db.PostgresGravidKravRepository
import no.nav.helse.fritakagp.db.PostgresGravidSoeknadRepository
import no.nav.helse.fritakagp.db.PostgresKroniskKravRepository
import no.nav.helse.fritakagp.db.PostgresKroniskSoeknadRepository
import no.nav.helse.fritakagp.db.SimpleJsonbEntity
import no.nav.helse.fritakagp.db.SimpleJsonbRepositoryBase
import no.nav.helse.fritakagp.processing.SlettUtdaterteJob
import no.nav.helse.mockGravidKrav
import no.nav.helse.mockGravidSoeknad
import no.nav.helse.mockKroniskKrav
import no.nav.helse.mockKroniskSoeknad
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import org.junit.jupiter.api.Test
import org.koin.core.component.get
import java.time.LocalDateTime

private data class Expected(
    val toDelete: List<LocalDateTime>,
    val toKeep: List<LocalDateTime>,
)

private object ExpectedData {
    val gravidSoeknad = Expected(
        toDelete = listOf(
            40.monthsInPast(),
            30.monthsInPast(),
            20.monthsInPast(),
        ),
        toKeep = listOf(
            10.monthsInPast(),
            2.monthsInFuture(),
        ),
    )

    val gravidKrav = Expected(
        toDelete = listOf(
            15.monthsInPast(),
            13.monthsInPast(),
        ),
        toKeep = listOf(
            11.monthsInPast(),
            6.monthsInPast(),
            6.monthsInFuture(),
            18.monthsInFuture(),
        ),
    )

    val kroniskSoeknad = Expected(
        toDelete = listOf(
            55.monthsInPast(),
            50.monthsInPast(),
            40.monthsInPast(),
        ),
        toKeep = listOf(
            30.monthsInPast(),
            20.monthsInPast(),
            10.monthsInPast(),
            5.monthsInPast(),
            5.monthsInFuture(),
        ),
    )

    val kroniskKrav = Expected(
        toDelete = listOf(
            37.monthsInPast(),
            37.monthsInPast(),
        ),
        toKeep = listOf(
            35.monthsInPast(),
            25.monthsInPast(),
            5.monthsInPast(),
            12.monthsInFuture(),
        ),
    )
}

class SlettUtdaterteJobTest : SystemTestBase() {
    @Test
    fun `jobb sletter soeknader og krav med for gamle 'opprettet'-datoer`() {
        val dataSource = createTestHikariDataSource()

        val repoGravidSoeknad = PostgresGravidSoeknadRepository(dataSource, get())
        val repoGravidKrav = PostgresGravidKravRepository(dataSource, get())
        val repoKroniskSoeknad = PostgresKroniskSoeknadRepository(dataSource, get())
        val repoKroniskKrav = PostgresKroniskKravRepository(dataSource, get())

        val job = SlettUtdaterteJob(
            repoGravidSoeknad,
            repoGravidKrav,
            repoKroniskSoeknad,
            repoKroniskKrav,
        )

        listOf(
            repoGravidSoeknad to ExpectedData.gravidSoeknad,
            repoGravidKrav to ExpectedData.gravidKrav,
            repoKroniskSoeknad to ExpectedData.kroniskSoeknad,
            repoKroniskKrav to ExpectedData.kroniskKrav,
        )
            .onEach { (repo, expected) ->
                repo.insertMocksMedOpprettetDato(expected.toDelete + expected.toKeep)
            }
            .also {
                // Utfør jobben som skal testes
                job.doJob()
            }
            .forEach { (repo, expected) ->
                repo.assertContainsOpprettetDatoer(expected.toKeep)
            }
    }
}

private fun <T : SimpleJsonbEntity> SimpleJsonbRepositoryBase<T>.insertMocksMedOpprettetDato(opprettetDatoer: List<LocalDateTime>) {
    opprettetDatoer
        .sumOf {
            when (this) {
                is PostgresGravidSoeknadRepository ->
                    mockGravidSoeknad().copy(opprettet = it).let(::insert)
                is PostgresGravidKravRepository ->
                    mockGravidKrav().copy(opprettet = it).let(::insert)
                is PostgresKroniskSoeknadRepository ->
                    mockKroniskSoeknad().copy(opprettet = it).let(::insert)
                is PostgresKroniskKravRepository ->
                    mockKroniskKrav().copy(opprettet = it).let(::insert)
            }
        }
        .assert { it.isEqualTo(opprettetDatoer.size) }
}

private fun <T : SimpleJsonbEntity> SimpleJsonbRepositoryBase<T>.assertContainsOpprettetDatoer(opprettetDatoer: List<LocalDateTime>) {
    getAll()
        .map { it.opprettet }
        .assert { it.containsExactlyInAnyOrderElementsOf(opprettetDatoer) }
}

private fun Int.monthsInPast(): LocalDateTime =
    LocalDateTime.now().minusMonths(this.toLong())

private fun Int.monthsInFuture(): LocalDateTime =
    LocalDateTime.now().plusMonths(this.toLong())
