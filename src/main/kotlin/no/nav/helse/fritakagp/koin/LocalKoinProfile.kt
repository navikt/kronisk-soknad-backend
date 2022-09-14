package no.nav.helse.fritakagp.koin

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
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
import no.nav.helse.fritakagp.integration.GrunnbeloepClient
import no.nav.helse.fritakagp.integration.kafka.KravmeldingKafkaProducer
import no.nav.helse.fritakagp.integration.kafka.KravmeldingSender
import no.nav.helse.fritakagp.integration.kafka.SoeknadmeldingKafkaProducer
import no.nav.helse.fritakagp.integration.kafka.SoeknadmeldingSender
import no.nav.helse.fritakagp.integration.kafka.StringKafkaProducerFactory
import no.nav.helse.fritakagp.integration.kafka.localCommonKafkaProps
import no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonProcessor
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKafkaProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKvitteringProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKvitteringSender
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKvitteringSenderDummy
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravPDFGenerator
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravSlettProcessor
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKafkaProcessor
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKvitteringProcessor
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKvitteringSender
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKvitteringSenderDummy
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadPDFGenerator
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKafkaProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKvitteringProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKvitteringSender
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKvitteringSenderDummy
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravPDFGenerator
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravSlettProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKafkaProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringSender
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringSenderDummy
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadPDFGenerator
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadProcessor
import no.nav.helse.fritakagp.service.AltinnService
import no.nav.helse.fritakagp.service.PdlService
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import java.net.URL
import javax.sql.DataSource

fun localDevConfig(config: ApplicationConfig): Module = module {
    mockExternalDependecies()
    single { GrunnbeloepClient(get()) }
    single { BeloepBeregning(get()) }
    single { HikariDataSource(createHikariConfig(config.jdbcUrl(), config.prop("database.username"), config.prop("database.password"))) } bind DataSource::class
    single { PostgresGravidSoeknadRepository(get(), get()) } bind GravidSoeknadRepository::class
    single { PostgresGravidKravRepository(get(), get()) } bind GravidKravRepository::class
    single { PostgresKroniskSoeknadRepository(get(), get()) } bind KroniskSoeknadRepository::class
    single { PostgresKroniskKravRepository(get(), get()) } bind KroniskKravRepository::class

    single { SoeknadmeldingKafkaProducer(localCommonKafkaProps(), config.prop("kafka_soeknad_topic_name"), get(), StringKafkaProducerFactory()) } bind SoeknadmeldingSender::class
    single { KravmeldingKafkaProducer(localCommonKafkaProps(), config.prop("kafka_krav_topic_name"), get(), StringKafkaProducerFactory()) } bind KravmeldingSender::class

    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { BakgrunnsjobbService(get()) }

    single { GravidSoeknadProcessor(get(), get(), get(), get(), get(), GravidSoeknadPDFGenerator(), get(), get(), get(), get()) }
    single { GravidKravProcessor(get(), get(), get(), get(), get(), GravidKravPDFGenerator(), get(), get(), get(), get()) }
    single { GravidKravSlettProcessor(get(), get(), get(), get(), get(), GravidKravPDFGenerator(), get(), get(), get(), get(), get()) }
    single { KroniskSoeknadProcessor(get(), get(), get(), get(), get(), KroniskSoeknadPDFGenerator(), get(), get(), get(), get()) }
    single { KroniskKravProcessor(get(), get(), get(), get(), get(), KroniskKravPDFGenerator(), get(), get(), get(), get()) }
    single { KroniskKravSlettProcessor(get(), get(), get(), get(), get(), KroniskKravPDFGenerator(), get(), get(), get(), get(), get()) }

    single { GravidSoeknadKvitteringSenderDummy() } bind GravidSoeknadKvitteringSender::class
    single { GravidSoeknadKvitteringProcessor(get(), get(), get()) }
    single { GravidKravKvitteringSenderDummy() } bind GravidKravKvitteringSender::class
    single { GravidKravKvitteringProcessor(get(), get(), get()) }

    single { KroniskSoeknadKvitteringSenderDummy() } bind KroniskSoeknadKvitteringSender::class
    single { KroniskSoeknadKvitteringProcessor(get(), get(), get()) }
    single { KroniskKravKvitteringSenderDummy() } bind KroniskKravKvitteringSender::class
    single { KroniskKravKvitteringProcessor(get(), get(), get()) }

    single { GravidSoeknadKafkaProcessor(get(), get(), get()) }
    single { GravidKravKafkaProcessor(get(), get(), get()) }
    single { KroniskSoeknadKafkaProcessor(get(), get(), get()) }
    single { KroniskKravKafkaProcessor(get(), get(), get()) }

    single { AltinnService(get()) }
    single { PdlService(get()) }

    single { BrukernotifikasjonProcessor(get(), get(), get(), get(), get(), get(), 4, "mock") }
    single { ArbeidsgiverNotifikasjonProcessor(get(), get(), get(), "https://mock.no", get()) }

    single { DatapakkePublisherJob(get(), get(), config.prop("datapakke.api_url"), config.prop("datapakke.id"), get()) }
    single { StatsRepoImpl(get()) } bind IStatsRepo::class

    single { ArbeidsgiverNotifikasjonKlient(URL(config.prop("arbeidsgiver_notifikasjon_api_url")), get()) { "fake token" } }
}
