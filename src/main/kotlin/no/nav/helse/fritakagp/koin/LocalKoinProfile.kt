package no.nav.helse.fritakagp.koin

import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.helse.arbeidsgiver.web.auth.AltinnAuthorizer
import no.nav.helse.arbeidsgiver.web.auth.DefaultAltinnAuthorizer
import no.nav.helse.fritakagp.datapakke.DatapakkePublisherJob
import no.nav.helse.fritakagp.db.*
import no.nav.helse.fritakagp.domain.BeloepBeregning
import no.nav.helse.fritakagp.integration.GrunnbeloepClient
import no.nav.helse.fritakagp.integration.kafka.*
import no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonProcessor
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.*
import no.nav.helse.fritakagp.processing.gravid.soeknad.*
import no.nav.helse.fritakagp.processing.kronisk.krav.*
import no.nav.helse.fritakagp.processing.kronisk.soeknad.*
import no.nav.helse.fritakagp.service.PdlService
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import org.koin.dsl.bind
import org.koin.dsl.module
import java.net.URL
import javax.sql.DataSource

fun localDevConfig(config: ApplicationConfig) = module {

    mockExternalDependecies()
    single { GrunnbeloepClient(get()) }
    single { BeloepBeregning(get()) }
    single { HikariDataSource(createHikariConfig(config.getjdbcUrlFromProperties(), config.getString("database.username"), config.getString("database.password"))) } bind DataSource::class
    single { PostgresGravidSoeknadRepository(get(), get()) } bind GravidSoeknadRepository::class
    single { PostgresGravidKravRepository(get(), get()) } bind GravidKravRepository::class
    single { PostgresKroniskSoeknadRepository(get(), get()) } bind KroniskSoeknadRepository::class
    single { PostgresKroniskKravRepository(get(), get()) } bind KroniskKravRepository::class

    single { SoeknadmeldingKafkaProducer(localCommonKafkaProps(), config.getString("kafka_soeknad_topic_name"), get(), StringKafkaProducerFactory()) } bind SoeknadmeldingSender::class
    single { KravmeldingKafkaProducer(localCommonKafkaProps(), config.getString("kafka_krav_topic_name"), get(), StringKafkaProducerFactory()) } bind KravmeldingSender::class

    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { BakgrunnsjobbService(get()) }

    single { GravidSoeknadProcessor(get(), get(), get(), get(), get(), GravidSoeknadPDFGenerator(), get(), get(), get(), get()) }
    single { GravidKravProcessor(get(), get(), get(), get(), get(), GravidKravPDFGenerator(), get(), get(), get(), get()) }
    single { SlettGravidKravProcessor(get(), get(), get(), get(), get(), GravidKravPDFGenerator(), get(), get(), get(), get(), get()) }
    single { KroniskSoeknadProcessor(get(), get(), get(), get(), get(), KroniskSoeknadPDFGenerator(), get(), get(), get(), get()) }
    single { KroniskKravProcessor(get(), get(), get(), get(), get(), KroniskKravPDFGenerator(), get(), get(), get(), get()) }
    single { SlettKroniskKravProcessor(get(), get(), get(), get(), get(), KroniskKravPDFGenerator(), get(), get(), get(), get(), get()) }

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

    single { GravidKravOppdaterNotifikasjonProcessor(get(), get(), get(), "https://localhost:3000/fritak-agp") }
    single { KroniskKravOppdaterNotifikasjonProcessor(get(), get(), get(), "https://localhost:3000/fritak-agp") }

    single { PdlService(get()) }

    single { BrukernotifikasjonProcessor(get(), get(), get(), get(), get(), get(), 4, "mock") }
    single { ArbeidsgiverNotifikasjonProcessor(get(), get(), get(), "https://mock.no", get()) }

    single { DefaultAltinnAuthorizer(get()) } bind AltinnAuthorizer::class

    single { DatapakkePublisherJob(get(), get(), config.getString("datapakke.api_url"), config.getString("datapakke.id"), get()) }
    single { StatsRepoImpl(get()) } bind IStatsRepo::class

    single { ArbeidsgiverNotifikasjonKlient(URL(config.getString("arbeidsgiver_notifikasjon_api_url")), get(), get()) }
}
