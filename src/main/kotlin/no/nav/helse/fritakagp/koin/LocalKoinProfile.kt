package no.nav.helse.fritakagp.koin

import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.web.auth.AltinnAuthorizer
import no.nav.helse.arbeidsgiver.web.auth.DefaultAltinnAuthorizer
import no.nav.helse.fritakagp.db.*
import no.nav.helse.fritakagp.integration.kafka.*
import no.nav.helse.fritakagp.processing.gravid.krav.*
import no.nav.helse.fritakagp.processing.gravid.soeknad.*
import no.nav.helse.fritakagp.processing.kronisk.krav.*
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadPDFGenerator
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringSenderDummy
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringSender
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource


@KtorExperimentalAPI
fun localDevConfig(config: ApplicationConfig) = module {

    mockExternalDependecies()

    single { HikariDataSource(createHikariConfig(config.getjdbcUrlFromProperties(), config.getString("database.username"), config.getString("database.password"))) } bind DataSource::class
    single { PostgresGravidSoeknadRepository(get(), get()) } bind GravidSoeknadRepository::class
    single { PostgresGravidKravRepository(get(), get()) } bind GravidKravRepository::class
    single { PostgresKroniskSoeknadRepository(get(), get()) } bind KroniskSoeknadRepository::class
    single { PostgresKroniskKravRepository(get(), get()) } bind KroniskKravRepository::class

    single { SoeknadmeldingKafkaProducer(producerLocalConfig(), "kafka_soeknad_topic_name", get())} bind SoeknadmeldingSender::class
    single { KravmeldingKafkaProducer(producerLocalConfig(), "kafka_krav_topic_name", get()) } bind KravmeldingSender::class

    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { BakgrunnsjobbService(get()) }

    single { GravidSoeknadProcessor(get(), get(), get(), get(), get(), GravidSoeknadPDFGenerator(), get(), get()) }
    single { GravidKravProcessor(get(), get(), get(), get(), get(), GravidKravPDFGenerator(), get(), get()) }
    single { KroniskSoeknadProcessor(get(), get(), get(), get(), get(), KroniskSoeknadPDFGenerator(), get(), get()) }
    single { KroniskKravProcessor(get(), get(), get(), get(), get(), KroniskKravPDFGenerator(), get(), get()) }

    single { GravidSoeknadKvitteringSenderDummy() } bind GravidSoeknadKvitteringSender::class
    single { GravidSoeknadKvitteringProcessor(get(), get(), get()) }
    single { GravidKravKvitteringSenderDummy() } bind GravidKravKvitteringSender::class
    single { GravidKravKvitteringProcessor(get(), get(), get()) }

    single { KroniskSoeknadKvitteringSenderDummy() } bind KroniskSoeknadKvitteringSender::class
    single { KroniskSoeknadKvitteringProcessor(get(), get(), get()) }
    single { KroniskKravKvitteringSenderDummy() } bind KroniskKravKvitteringSender::class
    single { KroniskKravKvitteringProcessor(get(), get(), get()) }

    single { DefaultAltinnAuthorizer(get()) } bind AltinnAuthorizer::class
}
