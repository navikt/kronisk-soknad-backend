package no.nav.helse.fritakagp.koin

import io.ktor.server.config.ApplicationConfig
import no.nav.helse.arbeidsgiver.integrasjoner.OAuth2TokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClient
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClientImpl
import no.nav.helse.fritakagp.config.prop
import no.nav.helse.fritakagp.integration.GrunnbeloepClient
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
import no.nav.helse.fritakagp.integration.oauth2.OAuth2ClientConfig
import no.nav.helse.fritakagp.integration.oauth2.TokenResolver
import no.nav.helse.fritakagp.integration.virusscan.ClamavVirusScannerImp
import no.nav.helse.fritakagp.integration.virusscan.VirusScanner
import no.nav.helse.fritakagp.service.BehandlendeEnhetService
import no.nav.helsearbeidsgiver.altinn.AltinnClient
import no.nav.helsearbeidsgiver.altinn.CacheConfig
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import kotlin.time.Duration.Companion.minutes

private enum class AuthScope(val scope: String) {
    PROXY("proxyscope"),
    DOKARK("dokarkivscope"),
    OPPGAVE("oppgavescope"),
    ARBEIDSGIVER_NOTIFIKASJON("arbeidsgivernotifikasjonscope"),
}

fun Module.externalSystemClients(config: ApplicationConfig) {
    single {
        AltinnClient(
            altinnBaseUrl = config.prop("altinn.service_owner_api_url"),
            serviceCode = config.prop("altinn.service_id"),
            apiGwApiKey = config.prop("altinn.gw_api_key"),
            altinnApiKey = config.prop("altinn.altinn_api_key"),
            httpClient = get(),
            cacheConfig = CacheConfig(60.minutes, 100),
        )
    }

    single { GrunnbeloepClient(get()) }

    single {
        AaregArbeidsforholdClientImpl(
            config.prop("aareg_url"),
            oAuth2TokenProvider(config, AuthScope.PROXY),
            get(),
        )
    } bind AaregArbeidsforholdClient::class

    single {
        PdlClientImpl(
            config.prop("pdl_url"),
            oAuth2TokenProvider(config, AuthScope.PROXY),
            get(),
            get(),
        )
    } bind PdlClient::class

    single {
        Norg2Client(
            config.prop("norg2_url"),
            oAuth2TokenProvider(config, AuthScope.PROXY),
            get(),
        )
    }

    single {
        DokarkivKlientImpl(
            config.prop("dokarkiv.base_url"),
            get(),
            oAuth2TokenProvider(config, AuthScope.DOKARK),
        )
    } bind DokarkivKlient::class

    single {
        OppgaveKlientImpl(
            config.prop("oppgavebehandling.url"),
            oAuth2TokenProvider(config, AuthScope.OPPGAVE),
            get(),
        )
    } bind OppgaveKlient::class

    single {
        val accessTokenProvider = oAuth2TokenProvider(config, AuthScope.ARBEIDSGIVER_NOTIFIKASJON)
        ArbeidsgiverNotifikasjonKlient(
            config.prop("arbeidsgiver_notifikasjon_api_url"),
            accessTokenProvider::getToken,
        )
    }

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

    single { BehandlendeEnhetService(get(), get()) }
}

private fun Scope.oAuth2TokenProvider(config: ApplicationConfig, authScope: AuthScope): OAuth2TokenProvider {
    val accessTokenService = DefaultOAuth2HttpClient(this.get()).let {
        OAuth2AccessTokenService(
            TokenResolver(),
            OnBehalfOfTokenClient(it),
            ClientCredentialsTokenClient(it),
            TokenExchangeClient(it)
        )
    }
    val azureAdConfig = OAuth2ClientConfig(config, authScope.scope)
        .clientConfig["azure_ad"]
        ?: error("Fant ikke config i application.conf")

    return OAuth2TokenProvider(accessTokenService, azureAdConfig)
}
