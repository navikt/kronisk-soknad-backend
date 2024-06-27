package no.nav.helse.fritakagp.koin

import com.zaxxer.hikari.HikariDataSource
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.hag.utils.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.fritakagp.Env
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.db.IStatsRepo
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helse.fritakagp.db.PostgresGravidKravRepository
import no.nav.helse.fritakagp.db.PostgresGravidSoeknadRepository
import no.nav.helse.fritakagp.db.PostgresKroniskKravRepository
import no.nav.helse.fritakagp.db.PostgresKroniskSoeknadRepository
import no.nav.helse.fritakagp.db.StatsRepoImpl
import no.nav.helse.fritakagp.db.createHikariConfig
import no.nav.helse.fritakagp.domain.BeloepBeregning
import no.nav.helse.fritakagp.integration.GrunnbeloepClient
import no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonProcessor
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravEndreProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKvitteringProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKvitteringSender
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKvitteringSenderDummy
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravPDFGenerator
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravSlettProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.OpprettRobotOppgaveGravidProcessor
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKvitteringProcessor
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKvitteringSender
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKvitteringSenderDummy
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadPDFGenerator
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravEndreProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKvitteringProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKvitteringSender
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKvitteringSenderDummy
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravPDFGenerator
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravSlettProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.OpprettRobotOppgaveKroniskProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringSender
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringSenderDummy
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadPDFGenerator
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadProcessor
import no.nav.helse.fritakagp.service.PdlService
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

fun localConfig(env: Env.Local): Module = module {
    mockExternalDependecies()

    single { GrunnbeloepClient(env.grunnbeloepUrl, get()) }
    single { BeloepBeregning(get()) }
    single { HikariDataSource(createHikariConfig(env.databaseUrl, env.databaseUsername, env.databasePassword)) } bind DataSource::class
    single { PostgresGravidSoeknadRepository(get(), get()) } bind GravidSoeknadRepository::class
    single { PostgresGravidKravRepository(get(), get()) } bind GravidKravRepository::class
    single { PostgresKroniskSoeknadRepository(get(), get()) } bind KroniskSoeknadRepository::class
    single { PostgresKroniskKravRepository(get(), get()) } bind KroniskKravRepository::class

    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { BakgrunnsjobbService(get()) }

    single { GravidSoeknadProcessor(get(), get(), get(), get(), get(), GravidSoeknadPDFGenerator(), get(), get(), get()) }
    single { GravidKravProcessor(get(), get(), get(), get(), get(), GravidKravPDFGenerator(), get(), get(), get()) }
    single { GravidKravSlettProcessor(get(), get(), get(), get(), GravidKravPDFGenerator(), get(), get()) }
    single { GravidKravEndreProcessor(get(), get(), get(), get(), GravidKravPDFGenerator(), get(), get()) }
    single { KroniskSoeknadProcessor(get(), get(), get(), get(), get(), KroniskSoeknadPDFGenerator(), get(), get(), get()) }
    single { KroniskKravProcessor(get(), get(), get(), get(), get(), KroniskKravPDFGenerator(), get(), get(), get()) }
    single { KroniskKravSlettProcessor(get(), get(), get(), get(), KroniskKravPDFGenerator(), get(), get()) }
    single { KroniskKravEndreProcessor(get(), get(), get(), get(), KroniskKravPDFGenerator(), get(), get()) }
    single { OpprettRobotOppgaveKroniskProcessor(get(), get(), get(), get()) }

    single { GravidSoeknadKvitteringSenderDummy() } bind GravidSoeknadKvitteringSender::class
    single { GravidSoeknadKvitteringProcessor(get(), get(), get()) }
    single { GravidKravKvitteringSenderDummy() } bind GravidKravKvitteringSender::class
    single { GravidKravKvitteringProcessor(get(), get(), get()) }
    single { OpprettRobotOppgaveGravidProcessor(get(), get(), get(), get()) }

    single { KroniskSoeknadKvitteringSenderDummy() } bind KroniskSoeknadKvitteringSender::class
    single { KroniskSoeknadKvitteringProcessor(get(), get(), get()) }
    single { KroniskKravKvitteringSenderDummy() } bind KroniskKravKvitteringSender::class
    single { KroniskKravKvitteringProcessor(get(), get(), get()) }

    single { PdlService(get()) }

    single { BrukernotifikasjonProcessor(get(), get(), get(), get(), get(), get(), 4, env.frontendUrl) }
    single { ArbeidsgiverNotifikasjonProcessor(get(), get(), get(), env.frontendUrl, get()) }

    single { StatsRepoImpl(get()) } bind IStatsRepo::class
    single { ArbeidsgiverNotifikasjonKlient(env.arbeidsgiverNotifikasjonUrl, getAccessToken = { "token" }) }
}
