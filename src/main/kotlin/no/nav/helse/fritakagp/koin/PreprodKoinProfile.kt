package no.nav.helse.fritakagp.koin

import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.*
import io.ktor.util.*
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.ServiceUserTokenXTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClientImpl
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.db.PostgresGravidSoeknadRepository
import no.nav.helse.fritakagp.db.createHikariConfig
import no.nav.helse.fritakagp.integrasjon.rest.sts.configureFor
import no.nav.helse.fritakagp.integrasjon.rest.sts.wsStsClient
import no.nav.helse.fritakagp.processing.gravid.GravidSoeknadPDFGenerator
import no.nav.helse.fritakagp.processing.gravid.SoeknadGravidProcessor
import no.nav.helse.fritakagp.tokenx.DefaultOAuth2HttpClient
import no.nav.helse.fritakagp.tokenx.TokenResolver
import no.nav.helse.fritakagp.tokenx.TokenXClientPropertiesConfig
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import no.nav.helse.fritakagp.processing.kvittering.AltinnKvitteringMapper
import no.nav.helse.fritakagp.processing.kvittering.AltinnKvitteringSender
import no.nav.helse.fritakagp.processing.kvittering.Clients
import no.nav.helse.fritakagp.processing.kvittering.KvitteringSender
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
    single {
        val altinnMeldingWsClient = Clients.iCorrespondenceExternalBasic(
                config.getString("altinn_melding.pep_gw_endpoint")
        )
        val sts = wsStsClient(
                config.getString("sts_url_ws"),
                config.getString("service_user.username") to config.getString("service_user.password")
        )
        sts.configureFor(altinnMeldingWsClient)
        altinnMeldingWsClient as ICorrespondenceAgencyExternalBasic
    }
    single {
        AltinnKvitteringSender(
                AltinnKvitteringMapper(config.getString("altinn_melding.service_id")),
                get(),
                config.getString("altinn_melding.username"),
                config.getString("altinn_melding.password"),
                get())
}
}

fun Module.externalSystemClients(config: ApplicationConfig) {

    single {
        val clientConfig = TokenXClientPropertiesConfig(config)
        val tokenResolver = TokenResolver()
        val oauthHttpClient = DefaultOAuth2HttpClient(get())
        val accessTokenService = OAuth2AccessTokenService(
            tokenResolver,
            OnBehalfOfTokenClient(oauthHttpClient),
            ClientCredentialsTokenClient(oauthHttpClient),
            TokenExchangeClient(oauthHttpClient)
        )

        val tokenxConfig = clientConfig.clientConfig["tokenx"] ?: error("Fant ikke config i application.conf")
        ServiceUserTokenXTokenProvider(accessTokenService, tokenxConfig)
    } bind AccessTokenProvider::class

    single { PdlClientImpl(config.getString("pdl_url"), get(), get(), get()) } bind PdlClient::class
    single { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get()) } bind DokarkivKlient::class
    single { OppgaveKlientImpl(config.getString("oppgavebehandling.url"), get(), get()) } bind OppgaveKlient::class
}

