package no.nav.helse.fritakagp.koin

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import no.nav.helse.arbeidsgiver.integrasjoner.OAuth2TokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClient
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlientImpl
import no.nav.helse.fritakagp.Env
import no.nav.helse.fritakagp.EnvOauth2
import no.nav.helse.fritakagp.integration.GrunnbeloepClient
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
import no.nav.helsearbeidsgiver.altinn.AltinnClient
import no.nav.helsearbeidsgiver.altinn.CacheConfig
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.brreg.BrregClient
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import java.net.URI
import kotlin.time.Duration.Companion.minutes

fun Module.externalSystemClients(env: Env, envOauth2: EnvOauth2) {
    single {
        AltinnClient(
            url = env.altinnServiceOwnerUrl,
            serviceCode = env.altinnServiceOwnerServiceId,
            apiGwApiKey = env.altinnServiceOwnerGatewayApiKey,
            altinnApiKey = env.altinnServiceOwnerApiKey,
            cacheConfig = CacheConfig(60.minutes, 100),
        )
    }

    single { GrunnbeloepClient(env.grunnbeloepUrl, get()) }

    single {
        AaregArbeidsforholdClientImpl(
            env.aaregUrl,
            oauth2TokenProvider(envOauth2, envOauth2.scopeProxy),
            get(),
        )
    } bind AaregArbeidsforholdClient::class

    single {
        val accessTokenProvider = oauth2TokenProvider(envOauth2, envOauth2.scopeProxy)
        PdlClient(
            env.pdlUrl,
            accessTokenProvider::getToken,
        )
    }

    single {
        Norg2Client(
            env.norg2Url,
            oauth2TokenProvider(envOauth2, envOauth2.scopeProxy),
            get(),
        )
    }

    single {
        DokarkivKlientImpl(
            env.dokarkivUrl,
            get(),
            oauth2TokenProvider(envOauth2, envOauth2.scopeDokarkiv),
        )
    } bind DokarkivKlient::class

    single {
        OppgaveKlientImpl(
            env.oppgavebehandlingUrl,
            oauth2TokenProvider(envOauth2, envOauth2.scopeOppgave),
            get(),
        )
    } bind OppgaveKlient::class

    single {
        val accessTokenProvider = oauth2TokenProvider(envOauth2, envOauth2.scopeArbeidsgivernotifikasjon)
        ArbeidsgiverNotifikasjonKlient(
            env.arbeidsgiverNotifikasjonUrl,
            accessTokenProvider::getToken,
        )
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

    single { BrregClient(env.brregUrl) }

    single { BehandlendeEnhetService(get(), get()) }
}

private fun Scope.oauth2TokenProvider(envOauth2: EnvOauth2, scope: String): OAuth2TokenProvider =
    OAuth2TokenProvider(
        oauth2Service = accessTokenService(this),
        clientProperties = envOauth2.azureAdConfig(scope)
    )

private fun accessTokenService(scope: Scope): OAuth2AccessTokenService =
    DefaultOAuth2HttpClient(scope.get()).let {
        OAuth2AccessTokenService(
            TokenResolver(),
            OnBehalfOfTokenClient(it),
            ClientCredentialsTokenClient(it),
            TokenExchangeClient(it)
        )
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
