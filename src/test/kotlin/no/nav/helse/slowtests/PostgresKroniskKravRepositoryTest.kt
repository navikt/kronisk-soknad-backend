package no.nav.helse.slowtests

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.fritakagp.db.PostgresKroniskKravRepository
import no.nav.helse.fritakagp.db.SimpleJsonbRepository
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.mockKroniskKrav
import java.time.LocalDateTime
import javax.sql.DataSource

class PostgresKroniskKravRepositoryTest : RepositoryTest<KroniskKrav>() {
    override fun instantiateRepo(ds: DataSource, om: ObjectMapper): SimpleJsonbRepository<KroniskKrav> =
        PostgresKroniskKravRepository(ds, om)

    override fun KroniskKrav.alteredCopy(): KroniskKrav =
        this.copy(
            identitetsnummer = "314",
            harVedlegg = true,
            antallDager = 15,
        )

    override fun mockEntity(opprettet: LocalDateTime, virksomhetsnummer: String?): KroniskKrav =
        mockKroniskKrav().let {
            it.copy(
                opprettet = opprettet,
                virksomhetsnummer = virksomhetsnummer ?: it.virksomhetsnummer,
            )
        }

    override fun arrayOf(vararg elements: KroniskKrav): Array<KroniskKrav> =
        kotlin.arrayOf(*elements)
}
