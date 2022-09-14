package no.nav.helse.fritakagp.koin

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.fritakagp.MetrikkVarsler
import no.nav.helse.fritakagp.config.jdbcUrl
import no.nav.helse.fritakagp.config.prop
import no.nav.helse.fritakagp.datapakke.DatapakkePublisherJob
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
import no.nav.helse.fritakagp.integration.altinn.Clients
import no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonProcessor
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravAltinnKvitteringSender
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKafkaProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKvitteringProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKvitteringSender
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravPDFGenerator
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravSlettProcessor
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadAltinnKvitteringSender
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKafkaProcessor
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKvitteringProcessor
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKvitteringSender
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadPDFGenerator
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravAltinnKvitteringSender
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKafkaProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKvitteringProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKvitteringSender
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravPDFGenerator
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravSlettProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadAltinnKvitteringSender
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKafkaProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringSender
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadPDFGenerator
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadProcessor
import no.nav.helse.fritakagp.service.AltinnService
import no.nav.helse.fritakagp.service.PdlService
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

fun prodConfig(config: ApplicationConfig): Module = module {
    externalSystemClients(config)

    single {
        HikariDataSource(
            createHikariConfig(
                config.jdbcUrl(),
                config.prop("database.username"),
                config.prop("database.password")
            )
        )
    } bind DataSource::class

    single { PostgresGravidSoeknadRepository(get(), get()) } bind GravidSoeknadRepository::class
    single { PostgresKroniskSoeknadRepository(get(), get()) } bind KroniskSoeknadRepository::class
    single { PostgresGravidKravRepository(get(), get()) } bind GravidKravRepository::class
    single { PostgresKroniskKravRepository(get(), get()) } bind KroniskKravRepository::class

    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { BakgrunnsjobbService(get(), bakgrunnsvarsler = MetrikkVarsler()) }

    single { GravidSoeknadProcessor(get(), get(), get(), get(), get(), GravidSoeknadPDFGenerator(), get(), get(), get(), get()) }
    single { GravidKravProcessor(get(), get(), get(), get(), get(), GravidKravPDFGenerator(), get(), get(), get(), get()) }
    single { GravidKravSlettProcessor(get(), get(), get(), get(), get(), GravidKravPDFGenerator(), get(), get(), get(), get(), get()) }

    single { KroniskSoeknadProcessor(get(), get(), get(), get(), get(), KroniskSoeknadPDFGenerator(), get(), get(), get(), get()) }
    single { KroniskKravProcessor(get(), get(), get(), get(), get(), KroniskKravPDFGenerator(), get(), get(), get(), get()) }
    single { KroniskKravSlettProcessor(get(), get(), get(), get(), get(), KroniskKravPDFGenerator(), get(), get(), get(), get(), get()) }

    single { Clients.iCorrespondenceExternalBasic(config.prop("altinn_melding.altinn_endpoint")) }

    single {
        GravidSoeknadAltinnKvitteringSender(
            config.prop("altinn_melding.service_id"),
            get(),
            config.prop("altinn_melding.username"),
            config.prop("altinn_melding.password")
        )
    } bind GravidSoeknadKvitteringSender::class

    single { GravidSoeknadKvitteringProcessor(get(), get(), get()) }

    single {
        GravidKravAltinnKvitteringSender(
            config.prop("altinn_melding.service_id"),
            get(),
            config.prop("altinn_melding.username"),
            config.prop("altinn_melding.password")
        )
    } bind GravidKravKvitteringSender::class

    single { GravidKravKvitteringProcessor(get(), get(), get()) }

    single {
        KroniskSoeknadAltinnKvitteringSender(
            config.prop("altinn_melding.service_id"),
            get(),
            config.prop("altinn_melding.username"),
            config.prop("altinn_melding.password")
        )
    } bind KroniskSoeknadKvitteringSender::class
    single { KroniskSoeknadKvitteringProcessor(get(), get(), get()) }

    single {
        KroniskKravAltinnKvitteringSender(
            config.prop("altinn_melding.service_id"),
            get(),
            config.prop("altinn_melding.username"),
            config.prop("altinn_melding.password")
        )
    } bind KroniskKravKvitteringSender::class
    single { KroniskKravKvitteringProcessor(get(), get(), get()) }

    single { GravidSoeknadKafkaProcessor(get(), get(), get()) }
    single { GravidKravKafkaProcessor(get(), get(), get()) }
    single { KroniskSoeknadKafkaProcessor(get(), get(), get()) }
    single { KroniskKravKafkaProcessor(get(), get(), get()) }

    single { BrukernotifikasjonProcessor(get(), get(), get(), get(), get(), get(), 4, "https://arbeidsgiver.nav.no/fritak-agp") }
    single { ArbeidsgiverNotifikasjonProcessor(get(), get(), get(), "https://arbeidsgiver.nav.no/fritak-agp", get()) }

    single { AltinnService(get()) }
    single { PdlService(get()) }

    single { BeloepBeregning(get()) }

    single { DatapakkePublisherJob(get(), get(), config.prop("datapakke.api_url"), config.prop("datapakke.id"), get()) }
    single { StatsRepoImpl(get()) } bind IStatsRepo::class
}
