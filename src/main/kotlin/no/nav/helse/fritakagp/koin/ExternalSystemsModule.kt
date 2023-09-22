package no.nav.helse.fritakagp.koin

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClient
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnRestClient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClientImpl
import no.nav.helse.arbeidsgiver.web.auth.AltinnOrganisationsRepository
import no.nav.helse.fritakagp.Env
import no.nav.helse.fritakagp.EnvOauth2
import no.nav.helse.fritakagp.integration.GrunnbeloepClient
import no.nav.helse.fritakagp.integration.altinn.CachedAuthRepo
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.integration.brreg.BrregClientImpl
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.integration.gcp.BucketStorageImpl
import no.nav.helse.fritakagp.integration.kafka.BrukernotifikasjonBeskjedKafkaProducer
import no.nav.helse.fritakagp.integration.kafka.BrukernotifikasjonBeskjedSender
import no.nav.helse.fritakagp.integration.kafka.KravmeldingKafkaProducer
import no.nav.helse.fritakagp.integration.kafka.KravmeldingSender
import no.nav.helse.fritakagp.integration.kafka.SoeknadmeldingKafkaProducer
import no.nav.helse.fritakagp.integration.kafka.SoeknadmeldingSender
import no.nav.helse.fritakagp.integration.kafka.StringKafkaProducerFactory
import no.nav.helse.fritakagp.integration.kafka.brukernotifikasjonKafkaProps
import no.nav.helse.fritakagp.integration.kafka.kravmeldingKafkaProps
import no.nav.helse.fritakagp.integration.kafka.soeknadmeldingKafkaProps
import no.nav.helse.fritakagp.integration.norg.Norg2Client
import no.nav.helse.fritakagp.integration.oauth2.DefaultOAuth2HttpClient
import no.nav.helse.fritakagp.integration.oauth2.TokenResolver
import no.nav.helse.fritakagp.integration.virusscan.ClamavVirusScannerImp
import no.nav.helse.fritakagp.integration.virusscan.VirusScanner
import no.nav.helse.fritakagp.service.BehandlendeEnhetService
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.dokarkiv.DokArkivClient
import no.nav.helsearbeidsgiver.tokenprovider.AccessTokenProvider
import no.nav.helsearbeidsgiver.tokenprovider.OAuth2TokenProvider
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.dsl.bind
import java.net.URI

fun Module.externalSystemClients(env: Env, envOauth2: EnvOauth2) {
    single {
        CachedAuthRepo(
            AltinnRestClient(
                env.altinnServiceOwnerUrl,
                env.altinnServiceOwnerGatewayApiKey,
                env.altinnServiceOwnerApiKey,
                env.altinnServiceOwnerServiceId,
                get()
            )
        )
    } bind AltinnOrganisationsRepository::class

    single { GrunnbeloepClient(env.grunnbeloepUrl, get()) }

    single(named("OPPGAVE")) {
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

    single(named("PROXY")) {
        val azureAdConfig = envOauth2.azureAdConfig(envOauth2.scopeProxy)
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

    single(named("DOKARKIV")) {
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

    single(named("ARBEIDSGIVERNOTIFIKASJON")) {
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

    single { AaregArbeidsforholdClientImpl(env.aaregUrl, get(qualifier = named("PROXY")), get()) } bind AaregArbeidsforholdClient::class
    single { PdlClientImpl(env.pdlUrl, get(qualifier = named("PROXY")), get(), get()) } bind PdlClient::class
    single {
        val tokenProvider: AccessTokenProvider = get(qualifier = named("DOKARKIV"))
        DokArkivClient(env.dokarkivUrl, tokenProvider::getToken)
    }
    single { OppgaveKlientImpl(env.oppgavebehandlingUrl, get(qualifier = named("OPPGAVE")), get()) } bind OppgaveKlient::class
    single {
        val tokenProvider: AccessTokenProvider = get(qualifier = named("ARBEIDSGIVERNOTIFIKASJON"))
        ArbeidsgiverNotifikasjonKlient(env.arbeidsgiverNotifikasjonUrl, tokenProvider::getToken)
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
        SoeknadmeldingKafkaProducer(
            soeknadmeldingKafkaProps(),
            env.kafkaTopicNameSoeknad,
            get(),
            StringKafkaProducerFactory()
        )
    } bind SoeknadmeldingSender::class

    single {
        KravmeldingKafkaProducer(
            kravmeldingKafkaProps(),
            env.kafkaTopicNameKrav,
            get(),
            StringKafkaProducerFactory()
        )
    } bind KravmeldingSender::class

    single {
        BrukernotifikasjonBeskjedKafkaProducer(
            brukernotifikasjonKafkaProps(),
            env.kafkaTopicNameBrukernotifikasjon
        )
    } bind BrukernotifikasjonBeskjedSender::class
    single { BrregClientImpl(get(), env.brregUrl) } bind BrregClient::class

    single { Norg2Client(env.norg2Url, get(qualifier = named("PROXY")), get()) }
    single { BehandlendeEnhetService(get(), get()) }
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
