package no.nav.helse.fritakagp.koin

import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.OAuth2TokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClient
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnRestClient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClientImpl
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.helse.arbeidsgiver.web.auth.AltinnOrganisationsRepository
import no.nav.helse.fritakagp.integration.GrunnbeløpClient
import no.nav.helse.fritakagp.integration.altinn.CachedAuthRepo
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.integration.brreg.BrregClientImpl
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.integration.gcp.BucketStorageImpl
import no.nav.helse.fritakagp.integration.kafka.*
import no.nav.helse.fritakagp.integration.norg.Norg2Client
import no.nav.helse.fritakagp.integration.oauth2.DefaultOAuth2HttpClient
import no.nav.helse.fritakagp.integration.oauth2.OAuth2ClientPropertiesConfig
import no.nav.helse.fritakagp.integration.oauth2.TokenResolver
import no.nav.helse.fritakagp.integration.virusscan.ClamavVirusScannerImp
import no.nav.helse.fritakagp.integration.virusscan.VirusScanner
import no.nav.helse.fritakagp.service.BehandlendeEnhetService
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind

@OptIn(KtorExperimentalAPI::class)
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

    single { GrunnbeløpClient(get()) }

    single(named("OPPGAVE")) {
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
    } bind AccessTokenProvider::class

    single(named("PROXY")) {
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
    } bind AccessTokenProvider::class

    single(named("DOKARKIV")) {
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
    } bind AccessTokenProvider::class

    single { AaregArbeidsforholdClientImpl(config.getString("aareg_url"), get(qualifier = named("PROXY")), get()) } bind AaregArbeidsforholdClient::class
    single { PdlClientImpl(config.getString("pdl_url"), get(qualifier = named("PROXY")), get(), get()) } bind PdlClient::class
    single { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get(qualifier = named("DOKARKIV"))) } bind DokarkivKlient::class
    single { OppgaveKlientImpl(config.getString("oppgavebehandling.url"), get(qualifier = named("OPPGAVE")), get()) } bind OppgaveKlient::class
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

    single {
        BrukernotifikasjonBeskjedKafkaProducer(
            brukernotifikasjonKafkaProps(),
            config.getString("brukernotifikasjon.topic_name")
        )
    } bind BrukernotifikasjonBeskjedSender::class
    single { BrregClientImpl(get(), config.getString("berreg_enhet_url")) } bind BrregClient::class

    single { Norg2Client(config.getString("norg2_url"), get(qualifier = named("PROXY")), get()) }
    single { BehandlendeEnhetService(get(), get()) }
}
