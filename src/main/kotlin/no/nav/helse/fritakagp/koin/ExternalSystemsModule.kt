package no.nav.helse.fritakagp.koin

import io.ktor.config.*
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.OAuth2TokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnRestClient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClientImpl
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.helse.arbeidsgiver.web.auth.AltinnOrganisationsRepository
import no.nav.helse.fritakagp.integration.altinn.CachedAuthRepo
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.integration.brreg.BrregClientImp
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.integration.gcp.BucketStorageImpl
import no.nav.helse.fritakagp.integration.kafka.*
import no.nav.helse.fritakagp.integration.oauth2.DefaultOAuth2HttpClient
import no.nav.helse.fritakagp.integration.oauth2.OAuth2ClientPropertiesConfig
import no.nav.helse.fritakagp.integration.oauth2.TokenResolver
import no.nav.helse.fritakagp.integration.virusscan.ClamavVirusScannerImp
import no.nav.helse.fritakagp.integration.virusscan.VirusScanner
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind

fun Module.externalSystemClients(config: ApplicationConfig) {
    val accessTokenProviderError = "Fant ikke config i application.conf"
    single {
        CachedAuthRepo(
            AltinnRestClient(
                config.getString("altinn.service_owner_api_url"),
                config.getString("altinn.gw_api_key"),
                config.getString("altinn.altinn_api_key"),
                config.getString("altinn.service_id"),
                get()
            )
        )
    } bind AltinnOrganisationsRepository::class

    single (named("PDL_SCOPE")){
        val clientConfig = OAuth2ClientPropertiesConfig(config, "pdlscope")
        val tokenResolver = TokenResolver()
        val oauthHttpClient = DefaultOAuth2HttpClient(get())
        val accessTokenService = OAuth2AccessTokenService(
            tokenResolver,
            OnBehalfOfTokenClient(oauthHttpClient),
            ClientCredentialsTokenClient(oauthHttpClient),
            TokenExchangeClient(oauthHttpClient)
        )

        val azureAdConfig = clientConfig.clientConfig["azure_ad"] ?: error(accessTokenProviderError)
        OAuth2TokenProvider(accessTokenService, azureAdConfig)
    }  bind AccessTokenProvider::class

    single (named("OPPGAVE_SCOPE")){
        val clientConfig = OAuth2ClientPropertiesConfig(config, "oppgavescope")
        val tokenResolver = TokenResolver()
        val oauthHttpClient = DefaultOAuth2HttpClient(get())
        val accessTokenService = OAuth2AccessTokenService(
            tokenResolver,
            OnBehalfOfTokenClient(oauthHttpClient),
            ClientCredentialsTokenClient(oauthHttpClient),
            TokenExchangeClient(oauthHttpClient)
        )

        val azureAdConfig = clientConfig.clientConfig["azure_ad"] ?: error(accessTokenProviderError)
        OAuth2TokenProvider(accessTokenService, azureAdConfig)
    }  bind AccessTokenProvider::class

    single (named("PROXY_SCOPE")){
        val clientConfig = OAuth2ClientPropertiesConfig(config, "proxyscope")
        val tokenResolver = TokenResolver()
        val oauthHttpClient = DefaultOAuth2HttpClient(get())
        val accessTokenService = OAuth2AccessTokenService(
            tokenResolver,
            OnBehalfOfTokenClient(oauthHttpClient),
            ClientCredentialsTokenClient(oauthHttpClient),
            TokenExchangeClient(oauthHttpClient)
        )

        val azureAdConfig = clientConfig.clientConfig["azure_ad"] ?: error(accessTokenProviderError)
        OAuth2TokenProvider(accessTokenService, azureAdConfig)
    }  bind AccessTokenProvider::class

    single (named("DOKARKIV_SCOPE")){
        val clientConfig = OAuth2ClientPropertiesConfig(config, "dokarkivscope")
        val tokenResolver = TokenResolver()
        val oauthHttpClient = DefaultOAuth2HttpClient(get())
        val accessTokenService = OAuth2AccessTokenService(
            tokenResolver,
            OnBehalfOfTokenClient(oauthHttpClient),
            ClientCredentialsTokenClient(oauthHttpClient),
            TokenExchangeClient(oauthHttpClient)
        )

        val azureAdConfig = clientConfig.clientConfig["azure_ad"] ?: error(accessTokenProviderError)
        OAuth2TokenProvider(accessTokenService, azureAdConfig)
    }  bind AccessTokenProvider::class

    single { PdlClientImpl(config.getString("pdl_url"), get(qualifier = named("PDL_SCOPE")), get(), get()) } bind PdlClient::class
    single { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get(qualifier = named("DOKARKIV_SCOPE"))) } bind DokarkivKlient::class
    single { OppgaveKlientImpl(config.getString("oppgavebehandling.url"), get(qualifier = named("OPPGAVE_SCOPE")), get()) } bind OppgaveKlient::class
    single {
        ClamavVirusScannerImp(
            get(),
            config.getString("clamav_url")
        )
    } bind VirusScanner::class
    single {
        BucketStorageImpl(
            config.getString("gcp_bucket_name"),
            config.getString("gcp_prjId")
        )
    } bind BucketStorage::class

    single { SoeknadmeldingKafkaProducer(gcpCommonKafkaProps(), config.getString("kafka_soeknad_topic_name"), get(), StringKafkaProducerFactory()) } bind SoeknadmeldingSender::class
    single { KravmeldingKafkaProducer(gcpCommonKafkaProps(), config.getString("kafka_krav_topic_name"), get(), StringKafkaProducerFactory()) } bind KravmeldingSender::class

    single { BrukernotifikasjonBeskjedKafkaProducer(
        onPremCommonKafkaProps(config),
        config.getString("brukernotifikasjon.topic_name"),
        BeskjedProducerFactory(config.getString("brukernotifikasjon.avro_schema_server_url"))
    )
    } bind BrukernotifikasjonBeskjedSender::class
    single { BrregClientImp(get(), get(), config.getString("berreg_enhet_url")) } bind BrregClient::class
}
