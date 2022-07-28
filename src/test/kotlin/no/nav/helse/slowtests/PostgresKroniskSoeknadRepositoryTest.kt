package no.nav.helse.slowtests

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.fritakagp.db.PostgresKroniskSoeknadRepository
import no.nav.helse.fritakagp.db.SimpleJsonbRepository
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import no.nav.helse.mockKroniskSoeknad
import java.time.LocalDateTime
import javax.sql.DataSource

class PostgresKroniskSoeknadRepositoryTest : RepositoryTest<KroniskSoeknad>() {
    override fun instantiateRepo(ds: DataSource, om: ObjectMapper): SimpleJsonbRepository<KroniskSoeknad> =
        PostgresKroniskSoeknadRepository(ds, om)

    override fun KroniskSoeknad.alteredCopy(): KroniskSoeknad =
        this.copy(
            journalpostId = "1234",
            oppgaveId = "78990",
            ikkeHistoriskFravaer = true,
        )

    override fun mockEntity(opprettet: LocalDateTime, virksomhetsnummer: String?): KroniskSoeknad =
        mockKroniskSoeknad().let {
            it.copy(
                opprettet = opprettet,
                virksomhetsnummer = virksomhetsnummer ?: it.virksomhetsnummer,
            )
        }

    override fun arrayOf(vararg elements: KroniskSoeknad): Array<KroniskSoeknad> =
        kotlin.arrayOf(*elements)
}
