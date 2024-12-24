package no.nav.helse.fritakagp.koin

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.OppgaveKlientImpl
import no.nav.helse.fritakagp.Env
import no.nav.helse.fritakagp.EnvOauth2
import no.nav.helse.fritakagp.auth.AuthClient
import no.nav.helse.fritakagp.auth.IdentityProvider
import no.nav.helse.fritakagp.auth.fetchToken
import no.nav.helse.fritakagp.integration.GrunnbeloepClient
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.integration.gcp.BucketStorageImpl
import no.nav.helse.fritakagp.integration.kafka.BrukernotifikasjonKafkaProducer
import no.nav.helse.fritakagp.integration.kafka.BrukernotifikasjonSender
import no.nav.helse.fritakagp.integration.kafka.brukernotifikasjonKafkaProps
import no.nav.helse.fritakagp.integration.oauth2.DefaultOAuth2HttpClient
import no.nav.helse.fritakagp.integration.oauth2.TokenResolver
import no.nav.helse.fritakagp.integration.virusscan.ClamavVirusScannerImp
import no.nav.helse.fritakagp.integration.virusscan.VirusScanner
import no.nav.helse.fritakagp.koin.AccessScope.AAREG
import no.nav.helse.fritakagp.koin.AccessScope.ARBEIDSGIVERNOTIFIKASJON
import no.nav.helse.fritakagp.koin.AccessScope.DOKARKIV
import no.nav.helse.fritakagp.koin.AccessScope.OPPGAVE
import no.nav.helse.fritakagp.koin.AccessScope.PDL
import no.nav.helsearbeidsgiver.aareg.AaregClient
import no.nav.helsearbeidsgiver.altinn.AltinnClient
import no.nav.helsearbeidsgiver.altinn.CacheConfig
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.dokarkiv.DokArkivClient
import no.nav.helsearbeidsgiver.pdl.Behandlingsgrunnlag
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.helsearbeidsgiver.tokenprovider.AccessTokenProvider
import no.nav.helsearbeidsgiver.tokenprovider.OAuth2TokenProvider
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import org.apache.cxf.ws.security.trust.STSTokenRetriever.getToken
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.QualifierValue
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import java.net.URI
import java.time.Duration
import kotlin.time.toKotlinDuration

enum class AccessScope : Qualifier {
    DOKARKIV,
    OPPGAVE,
    ARBEIDSGIVERNOTIFIKASJON,
    PDL,
    AAREG
    ;

    override val value: QualifierValue
        get() = name
}

fun Module.externalSystemClients(env: Env, envOauth2: EnvOauth2) {
    single {
        val maskinportenAuthClient: AuthClient = get()

        AltinnClient(
            url = env.altinnServiceOwnerUrl,
            serviceCode = env.altinnServiceOwnerServiceId,
            getToken = maskinportenAuthClient.fetchToken(env.altinnScope, IdentityProvider.MASKINPORTEN),
            altinnApiKey = env.altinnServiceOwnerApiKey,
            cacheConfig = CacheConfig(Duration.ofMinutes(60).toKotlinDuration(), 100)
        )
    } bind AltinnClient::class

    single { GrunnbeloepClient(env.grunnbeloepUrl, get()) }

    single(named(OPPGAVE)) {
        val azureAdConfig = envOauth2.azureAdConfig(envOauth2.scopeOppgave)
        val tokenResolver = TokenResolver()
        val oauthHttpClient = DefaultOAuth2HttpClient(get())
        val accessTokenService = OAuth2AccessTokenService(
            tokenResolver,
            OnBehalfOfTokenClient(oauthHttpClient),
            ClientCredentialsTokenClient(oauthHttpClient),
            TokenExchangeClient(oauthHttpClient)
        )

        OAuth2TokenProvider(accessTokenService, azureAdConfig)
    } bind AccessTokenProvider::class

    single(named(DOKARKIV)) {
        val azureAdConfig = envOauth2.azureAdConfig(envOauth2.scopeDokarkiv)
        val tokenResolver = TokenResolver()
        val oauthHttpClient = DefaultOAuth2HttpClient(get())
        val accessTokenService = OAuth2AccessTokenService(
            tokenResolver,
            OnBehalfOfTokenClient(oauthHttpClient),
            ClientCredentialsTokenClient(oauthHttpClient),
            TokenExchangeClient(oauthHttpClient)
        )

        OAuth2TokenProvider(accessTokenService, azureAdConfig)
    } bind AccessTokenProvider::class

    single(named(ARBEIDSGIVERNOTIFIKASJON)) {
        val azureAdConfig = envOauth2.azureAdConfig(envOauth2.scopeArbeidsgivernotifikasjon)
        val tokenResolver = TokenResolver()
        val oauthHttpClient = DefaultOAuth2HttpClient(get())
        val accessTokenService = OAuth2AccessTokenService(
            tokenResolver,
            OnBehalfOfTokenClient(oauthHttpClient),
            ClientCredentialsTokenClient(oauthHttpClient),
            TokenExchangeClient(oauthHttpClient)
        )
        OAuth2TokenProvider(accessTokenService, azureAdConfig)
    } bind AccessTokenProvider::class

    single(named(PDL)) {
        val azureAdConfig = envOauth2.azureAdConfig(envOauth2.scopePdl)
        val tokenResolver = TokenResolver()
        val oauthHttpClient = DefaultOAuth2HttpClient(get())
        val accessTokenService = OAuth2AccessTokenService(
            tokenResolver,
            OnBehalfOfTokenClient(oauthHttpClient),
            ClientCredentialsTokenClient(oauthHttpClient),
            TokenExchangeClient(oauthHttpClient)
        )

        OAuth2TokenProvider(accessTokenService, azureAdConfig)
    } bind AccessTokenProvider::class

    single(named(AAREG)) {
        val azureAdConfig = envOauth2.azureAdConfig(envOauth2.scopeAareg)
        val tokenResolver = TokenResolver()
        val oauthHttpClient = DefaultOAuth2HttpClient(get())
        val accessTokenService = OAuth2AccessTokenService(
            tokenResolver,
            OnBehalfOfTokenClient(oauthHttpClient),
            ClientCredentialsTokenClient(oauthHttpClient),
            TokenExchangeClient(oauthHttpClient)
        )

        OAuth2TokenProvider(accessTokenService, azureAdConfig)
    } bind AccessTokenProvider::class

    single {
        val azureAuthClient: AuthClient = get()
        PdlClient(env.pdlUrl, Behandlingsgrunnlag.FRITAKAGP, azureAuthClient.fetchToken(envOauth2.scopePdl, IdentityProvider.AZURE_AD))
    } bind PdlClient::class

    single {
        val azureAuthClient: AuthClient = get()
        AaregClient(env.aaregUrl, azureAuthClient.fetchToken(envOauth2.scopeAareg, IdentityProvider.AZURE_AD))
    } bind AaregClient::class

    single {
        val azureAuthClient: AuthClient = get()
        DokArkivClient(env.dokarkivUrl, 3, azureAuthClient.fetchToken(envOauth2.scopeDokarkiv, IdentityProvider.AZURE_AD))
    }
    single { OppgaveKlientImpl(env.oppgavebehandlingUrl, get(qualifier = named(OPPGAVE)), get()) } bind OppgaveKlient::class
    single {
        val azureAuthClient: AuthClient = get()
        ArbeidsgiverNotifikasjonKlient(env.arbeidsgiverNotifikasjonUrl, azureAuthClient.fetchToken(envOauth2.scopeArbeidsgivernotifikasjon, IdentityProvider.AZURE_AD))
    }
    single {
        ClamavVirusScannerImp(
            get(),
            env.clamAvUrl
        )
    } bind VirusScanner::class
    single {
        BucketStorageImpl(
            env.gcpBucketName,
            env.gcpProjectId
        )
    } bind BucketStorage::class

    single {
        BrukernotifikasjonKafkaProducer(
            brukernotifikasjonKafkaProps(),
            env.kafkaTopicNameBrukernotifikasjon
        )
    } bind BrukernotifikasjonSender::class

    single { AuthClient(env = env) }
}

private fun EnvOauth2.azureAdConfig(scope: String): ClientProperties =
    ClientProperties(
        tokenEndpointUrl.let(::URI),
        wellKnownUrl.let(::URI),
        grantType.let(::OAuth2GrantType),
        scope.split(","),
        authProps(),
        null,
        null
    )

private fun EnvOauth2.authProps(): ClientAuthenticationProperties =
    ClientAuthenticationProperties(
        authClientId,
        authClientAuthMethod.let(::ClientAuthenticationMethod),
        authClientSecret,
        null
    )
