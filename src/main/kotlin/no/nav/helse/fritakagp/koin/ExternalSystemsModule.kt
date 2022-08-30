package no.nav.helse.fritakagp.koin

import io.ktor.server.config.ApplicationConfig
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
import no.nav.helse.arbeidsgiver.web.auth.AltinnOrganisationsRepository
import no.nav.helse.fritakagp.config.prop
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
import no.nav.helse.fritakagp.integration.oauth2.OAuth2ClientPropertiesConfig
import no.nav.helse.fritakagp.integration.oauth2.TokenResolver
import no.nav.helse.fritakagp.integration.virusscan.ClamavVirusScannerImp
import no.nav.helse.fritakagp.integration.virusscan.VirusScanner
import no.nav.helse.fritakagp.service.BehandlendeEnhetService
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import java.net.URL

fun Module.externalSystemClients(config: ApplicationConfig) {
    val accessTokenProviderError = "Fant ikke config i application.conf"

    single {
        CachedAuthRepo(
            AltinnRestClient(
                config.prop("altinn.service_owner_api_url"),
                config.prop("altinn.gw_api_key"),
                config.prop("altinn.altinn_api_key"),
                config.prop("altinn.service_id"),
                get()
            )
        )
    } bind AltinnOrganisationsRepository::class

    single { GrunnbeloepClient(get()) }

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

    single(named("ARBEIDSGIVERNOTIFIKASJON")) {
        val clientConfig = OAuth2ClientPropertiesConfig(config, "arbeidsgivernotifikasjonscope")
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

    single { AaregArbeidsforholdClientImpl(config.prop("aareg_url"), get(qualifier = named("PROXY")), get()) } bind AaregArbeidsforholdClient::class
    single { PdlClientImpl(config.prop("pdl_url"), get(qualifier = named("PROXY")), get(), get()) } bind PdlClient::class
    single { DokarkivKlientImpl(config.prop("dokarkiv.base_url"), get(), get(qualifier = named("DOKARKIV"))) } bind DokarkivKlient::class
    single { OppgaveKlientImpl(config.prop("oppgavebehandling.url"), get(qualifier = named("OPPGAVE")), get()) } bind OppgaveKlient::class
    single { ArbeidsgiverNotifikasjonKlient(URL(config.prop("arbeidsgiver_notifikasjon_api_url")), get(qualifier = named("ARBEIDSGIVERNOTIFIKASJON")), get()) }
    single {
        ClamavVirusScannerImp(
            get(),
            config.prop("clamav_url")
        )
    } bind VirusScanner::class
    single {
        BucketStorageImpl(
            config.prop("gcp_bucket_name"),
            config.prop("gcp_prjId")
        )
    } bind BucketStorage::class

    single {
        SoeknadmeldingKafkaProducer(
            soeknadmeldingKafkaProps(),
            config.prop("kafka_soeknad_topic_name"),
            get(),
            StringKafkaProducerFactory()
        )
    } bind SoeknadmeldingSender::class

    single {
        KravmeldingKafkaProducer(
            kravmeldingKafkaProps(),
            config.prop("kafka_krav_topic_name"),
            get(),
            StringKafkaProducerFactory()
        )
    } bind KravmeldingSender::class

    single {
        BrukernotifikasjonBeskjedKafkaProducer(
            brukernotifikasjonKafkaProps(),
            config.prop("brukernotifikasjon.topic_name")
        )
    } bind BrukernotifikasjonBeskjedSender::class
    single { BrregClientImpl(get(), config.prop("berreg_enhet_url")) } bind BrregClient::class

    single { Norg2Client(config.prop("norg2_url"), get(qualifier = named("PROXY")), get()) }
    single { BehandlendeEnhetService(get(), get()) }
}
