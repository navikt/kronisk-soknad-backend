package no.nav.helse.slowtests

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.fritakagp.db.PostgresGravidKravRepository
import no.nav.helse.fritakagp.db.SimpleJsonbRepository
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.mockGravidKrav
import java.time.LocalDateTime
import javax.sql.DataSource

class PostgresGravidKravRepositoryTest : RepositoryTest<GravidKrav>() {
    override fun instantiateRepo(ds: DataSource, om: ObjectMapper): SimpleJsonbRepository<GravidKrav> =
        PostgresGravidKravRepository(ds, om)

    override fun GravidKrav.alteredCopy(): GravidKrav =
        this.copy(
            antallDager = 10,
            journalpostId = "123",
            slettetAv = "Sauron",
        )

    override fun mockEntity(opprettet: LocalDateTime, virksomhetsnummer: String?): GravidKrav =
        mockGravidKrav().let {
            it.copy(
                opprettet = opprettet,
                virksomhetsnummer = virksomhetsnummer ?: it.virksomhetsnummer,
            )
        }

    override fun arrayOf(vararg elements: GravidKrav): Array<GravidKrav> =
        kotlin.arrayOf(*elements)
}
