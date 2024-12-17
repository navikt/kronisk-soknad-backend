package no.nav.helse.fritakagp

import io.ktor.server.config.ApplicationConfig

fun readEnv(config: ApplicationConfig): Env =
    when (config.prop("koin.profile")) {
        "PROD" -> Env::Prod
        "PREPROD" -> Env::Preprod
        else -> Env::Local
    }
        .invoke(config)

sealed class Env private constructor(
    internal val config: ApplicationConfig
) {
    class Prod(config: ApplicationConfig) : Env(config) {
        val oauth2 = EnvOauth2(config)
    }

    class Preprod(config: ApplicationConfig) : Env(config) {
        val oauth2 = EnvOauth2(config)
        val jwt = EnvJwt(config)
    }

    class Local(config: ApplicationConfig) : Env(config) {
        val jwt = EnvJwt(config)
    }

    val ktorBasepath = "ktor.application.basepath".prop()

    val frontendUrl = "frontend_app_url".prop()

    val databaseUrl = "jdbc:postgresql://%s:%s/%s".format("database.host".prop(), "database.port".prop(), "database.name".prop())
    val databaseUsername = "database.username".prop()
    val databasePassword = "database.password".prop()

    val gcpBucketName = "gcp_bucket_name".prop()
    val gcpProjectId = "gcp_prjId".prop()

    val kafkaTopicNameBrukernotifikasjon = "brukernotifikasjon.topic_name".prop()

    // Integrasjoner (URL)

    val altinnMeldingUrl = "altinn_melding.altinn_endpoint".prop()
    val altinnMeldingServiceId = "altinn_melding.service_id".prop()
    val altinnMeldingUsername = "altinn_melding.username".prop()
    val altinnMeldingPassword = "altinn_melding.password".prop()

    val altinnServiceOwnerUrl = "altinn.service_owner_api_url".prop()
    val altinnServiceOwnerServiceId = "altinn.service_id".prop()
    val altinnServiceOwnerApiKey = "altinn.altinn_api_key".prop()
    val altinnScope = "altinn.altinn_scope".prop()

    val maskinportenUrl = "maskinporten.token_endpoint".prop()
    val maskinportenClientId = "maskinporten.client_id".prop()
    val maskinportenClientJwk = "maskinporten.client_jwk".prop()
    val maskinportenIssuer = "maskinporten.client_issuer".prop()

    val tokenEndpoint = "auth.token_endpoint".prop()
    val tokenExchangeEndpoint = "auth.token_exchange_endpoint".prop()
    val tokenIntrospectionEndpoint = "auth.token_introspection_endpoint".prop()

    val aaregUrl = "aareg_url".prop()

    val arbeidsgiverNotifikasjonUrl = "arbeidsgiver_notifikasjon_api_url".prop()

    val brregUrl = "brreg_enhet_url".prop()

    val clamAvUrl = "clamav_url".prop()

    val dokarkivUrl = "dokarkiv.base_url".prop()

    val grunnbeloepUrl = "grunnbeloep_url".prop()

    val oppgavebehandlingUrl = "oppgavebehandling.url".prop()

    val pdlUrl = "pdl_url".prop()

    private fun String.prop(): String =
        config.prop(this)
}

class EnvOauth2(mainConfig: ApplicationConfig) {
    private val oauth2Config = "no.nav.security.jwt.client.registration.clients".let(mainConfig::configList)
        .first {
            it.prop("client_name") == "azure_ad"
        }

    val tokenEndpointUrl = "token_endpoint_url".prop()
    val wellKnownUrl = "well_known_url".prop()
    val grantType = "grant_type".prop()

    val authClientId = "authentication.client_id".prop()
    val authClientAuthMethod = "authentication.client_auth_method".prop()
    val authClientSecret = "authentication.client_secret".prop()

    val scopeOppgave = "oppgavescope".prop()
    val scopeDokarkiv = "dokarkivscope".prop()
    val scopeArbeidsgivernotifikasjon = "arbeidsgivernotifikasjonscope".prop()
    val scopePdl = "pdlscope".prop()
    val scopeAareg = "aaregscope".prop()

    private fun String.prop(): String =
        oauth2Config.prop(this)
}

class EnvJwt(mainConfig: ApplicationConfig) {
    private val jwtIssuerConfig = "no.nav.security.jwt.issuers".let(mainConfig::configList)
        .first()

    val issuerName = "issuer_name".prop()
    val audience = "accepted_audience".prop()

    private fun String.prop(): String =
        jwtIssuerConfig.prop(this)
}

private fun ApplicationConfig.prop(key: String): String =
    property(key).getString()
