package no.nav.helse.slowtests

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.fritakagp.db.PostgresGravidSoeknadRepository
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.domain.Tiltak
import no.nav.helse.mockGravidSoeknad
import java.time.LocalDateTime
import javax.sql.DataSource

class PostgresGravidSoeknadRepositoryTest : RepositoryTest<GravidSoeknad>() {
    override fun instantiateRepo(ds: DataSource, om: ObjectMapper): PostgresGravidSoeknadRepository =
        PostgresGravidSoeknadRepository(ds, om)

    override fun GravidSoeknad.alteredCopy(): GravidSoeknad =
        this.copy(
            navn = "Samwise Gamgee",
            tiltak = listOf(
                Tiltak.HJEMMEKONTOR,
                Tiltak.TILPASSEDE_ARBEIDSOPPGAVER,
            ),
            journalpostId = "123"
        )

    override fun mockEntity(
        opprettet: LocalDateTime,
        virksomhetsnummer: String?,
    ): GravidSoeknad =
        mockGravidSoeknad().let {
            it.copy(
                opprettet = opprettet,
                virksomhetsnummer = virksomhetsnummer ?: it.virksomhetsnummer,
            )
        }

    override fun arrayOf(vararg elements: GravidSoeknad): Array<GravidSoeknad> =
        kotlin.arrayOf(*elements)
}
