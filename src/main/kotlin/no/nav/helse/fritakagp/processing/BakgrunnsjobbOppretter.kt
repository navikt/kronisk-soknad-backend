package no.nav.helse.fritakagp.processing

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.domain.KravStatus
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKvitteringProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.SlettGravidKravProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKvitteringProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.SlettKroniskKravProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringProcessor
import java.time.LocalDateTime
import javax.sql.DataSource

class BakgrunnsjobbOppretter(
    private val datasource: DataSource,
    private val bakgunnsjobbService: BakgrunnsjobbService,
    private val gravidKravRepo: GravidKravRepository,
    private val gravidSoeknadRepo: GravidSoeknadRepository,
    private val kroniskSoeknadRepo: KroniskSoeknadRepository,
    private val kroniskKravRepo: KroniskKravRepository,
    private val om: ObjectMapper

) {
    fun gravidSoeknadBakgrunnsjobb(soeknad: GravidSoeknad) {
        datasource.connection.use { connection ->
            gravidSoeknadRepo.insert(soeknad, connection)
            bakgunnsjobbService.opprettJobb<GravidKravProcessor>(
                maksAntallForsoek = 10,
                data = om.writeValueAsString(GravidKravProcessor.JobbData(soeknad.id)),
                connection = connection
            )
        }
    }
    fun gravidSoeknadKvitteringBakgrunnsjobb(soeknad: GravidSoeknad) {
        datasource.connection.use { connection ->
            bakgunnsjobbService.opprettJobb<GravidKravKvitteringProcessor>(
                maksAntallForsoek = 10,
                data = om.writeValueAsString(GravidKravKvitteringProcessor.Jobbdata(soeknad.id)),
                connection = connection
            )
        }
    }

    fun gravidKravBakgrunnsjobb(krav: GravidKrav) {
        datasource.connection.use { connection ->
            gravidKravRepo.insert(krav, connection)
            bakgunnsjobbService.opprettJobb<GravidKravProcessor>(
                maksAntallForsoek = 10,
                data = om.writeValueAsString(GravidKravProcessor.JobbData(krav.id)),
                connection = connection
            )
        }
    }
    fun gravidKravKvitteringBakgrunnsjobb(krav: GravidKrav) {
        datasource.connection.use { connection ->
            bakgunnsjobbService.opprettJobb<GravidKravKvitteringProcessor>(
                maksAntallForsoek = 10,
                data = om.writeValueAsString(GravidKravKvitteringProcessor.Jobbdata(krav.id)),
                connection = connection
            )
        }
    }

    fun gravidKravEndretBakgrunnsjobb(status: KravStatus, innloggetFnr: String, slettetAv: String, krav: GravidKrav) {
        krav.status = status
        krav.slettetAv = innloggetFnr
        krav.slettetAvNavn = slettetAv
        krav.endretDato = LocalDateTime.now()
        datasource.connection.use { connection ->
            gravidKravRepo.update(krav, connection)
            bakgunnsjobbService.opprettJobb<SlettGravidKravProcessor>(
                maksAntallForsoek = 10,
                data = om.writeValueAsString(GravidKravProcessor.JobbData(krav.id)),
                connection = connection
            )
        }
    }

    fun kroniskSoeknadBakgrunnsjobb(soeknad: KroniskSoeknad) {
        datasource.connection.use { connection ->
            kroniskSoeknadRepo.insert(soeknad, connection)
            bakgunnsjobbService.opprettJobb<GravidKravProcessor>(
                maksAntallForsoek = 10,
                data = om.writeValueAsString(GravidKravProcessor.JobbData(soeknad.id)),
                connection = connection
            )
        }
    }
    fun kroniskSoeknadKvitteringBakgrunnsjobb(soeknad: KroniskSoeknad) {
        datasource.connection.use { connection ->
            bakgunnsjobbService.opprettJobb<KroniskSoeknadKvitteringProcessor>(
                maksAntallForsoek = 10,
                data = om.writeValueAsString(KroniskSoeknadKvitteringProcessor.Jobbdata(soeknad.id)),
                connection = connection
            )
        }
    }

    fun kroniskKravBakgrunnsjobb(krav: KroniskKrav) {
        datasource.connection.use { connection ->
            kroniskKravRepo.insert(krav, connection)
            bakgunnsjobbService.opprettJobb<KroniskKravProcessor>(
                maksAntallForsoek = 10,
                data = om.writeValueAsString(KroniskKravProcessor.JobbData(krav.id)),
                connection = connection
            )
        }
    }
    fun kroniskKravKvitteringBakgrunnsjobb(krav: KroniskKrav) {
        datasource.connection.use { connection ->
            bakgunnsjobbService.opprettJobb<KroniskKravKvitteringProcessor>(
                maksAntallForsoek = 10,
                data = om.writeValueAsString(KroniskKravKvitteringProcessor.Jobbdata(krav.id)),
                connection = connection
            )
        }
    }
    fun kroniskKravEndretBakgrunnsjobb(status: KravStatus, innloggetFnr: String, slettetAv: String, krav: KroniskKrav) {
        krav.status = status
        krav.slettetAv = innloggetFnr
        krav.slettetAvNavn = slettetAv
        krav.endretDato = LocalDateTime.now()
        datasource.connection.use { connection ->
            kroniskKravRepo.update(krav, connection)
            bakgunnsjobbService.opprettJobb<SlettKroniskKravProcessor>(
                maksAntallForsoek = 10,
                data = om.writeValueAsString(KroniskKravProcessor.JobbData(krav.id)),
                connection = connection
            )
        }
    }
}
