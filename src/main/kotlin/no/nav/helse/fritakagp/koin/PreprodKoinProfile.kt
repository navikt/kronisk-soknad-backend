package no.nav.helse.fritakagp.koin

import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.integrasjoner.RestStsClient
import no.nav.helse.arbeidsgiver.integrasjoner.RestStsClientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClientImpl
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.db.PostgresGravidSoeknadRepository
import no.nav.helse.fritakagp.db.createHikariConfig
import no.nav.helse.fritakagp.processing.gravid.GravidSoeknadPDFGenerator
import no.nav.helse.fritakagp.processing.gravid.SoeknadGravidProcessor
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource


@KtorExperimentalAPI
fun preprodConfig(config: ApplicationConfig) = module {
    externalSystemClients(config)

    single { HikariDataSource(createHikariConfig(config.getjdbcUrlFromProperties(), config.getString("database.username"), config.getString("database.password"))) } bind DataSource::class
    single { PostgresGravidSoeknadRepository(get(), get()) } bind GravidSoeknadRepository::class


    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { BakgrunnsjobbService(get()) }

    single { SoeknadGravidProcessor(get(), get(), get(), get(), GravidSoeknadPDFGenerator(), get()) }
}


fun Module.externalSystemClients(config: ApplicationConfig) {
    single { object: RestStsClient {
            // STS er ikke tilgjengelig i GCP, og vi bruker en proxy for å snakke med tjenester i FSS
            override fun getOidcToken(): String {
                return "dummy-token-for-proxy"
            }
        }
    } bind RestStsClient::class


    single { PdlClientImpl(config.getString("pdl_url"), get(), get(), get()) } bind PdlClient::class
    single { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get()) } bind DokarkivKlient::class
    single { OppgaveKlientImpl(config.getString("oppgavebehandling.url"), get(), get()) } bind OppgaveKlient::class
}