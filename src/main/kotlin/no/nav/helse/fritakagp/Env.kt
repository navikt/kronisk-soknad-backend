package no.nav.helse.fritakagp

import io.ktor.config.ApplicationConfig

class Env(val config: ApplicationConfig) {
    val appEnv = when ("koin.profile".prop()) {
        "PROD" -> AppEnv.PROD
        "PREPROD" -> AppEnv.PREPROD
        else -> AppEnv.LOCAL
    }
    val appShouldRunBackgroundWorkers = "run_background_workers".prop() == "true"

    val frontendUrl = "frontend_app_url".prop()

    val databaseUrl = "jdbc:postgresql://%s:%s/%s".format("database.host".prop(), "database.port".prop(), "database.name".prop())
    val databaseUsername = "database.username".prop()
    val databasePassword = "database.password".prop()

    val gcpBucketName = "gcp_bucket_name".prop()
    val gcpProjectId = "gcp_prjId".prop()

    val kafkaTopicNameBrukernotifikasjon = "brukernotifikasjon.topic_name".prop()
    val kafkaTopicNameSoeknad = "kafka_soeknad_topic_name".prop()
    val kafkaTopicNameKrav = "kafka_krav_topic_name".prop()

    // Integrasjoner (URL)

    val altinnMeldingUrl = "altinn_melding.altinn_endpoint".prop()
    val altinnMeldingServiceId = "altinn_melding.service_id".prop()
    val altinnMeldingUsername = "altinn_melding.username".prop()
    val altinnMeldingPassword = "altinn_melding.password".prop()

    val altinnServiceOwnerUrl = "altinn.service_owner_api_url".prop()
    val altinnServiceOwnerServiceId = "altinn.service_id".prop()
    val altinnServiceOwnerApiKey = "altinn.altinn_api_key".prop()
    val altinnServiceOwnerGatewayApiKey = "altinn.gw_api_key".prop()

    val datapakkeUrl = "datapakke.api_url".prop()
    val datapakkeId = "datapakke.id".prop()

    val aaregUrl = "aareg_url".prop()

    val arbeidsgiverNotifikasjonUrl = "arbeidsgiver_notifikasjon_api_url".prop()

    val brregUrl = "brreg_enhet_url".prop() // TODO rename i app.config

    val clamAvUrl = "clamav_url".prop()

    val dokarkivUrl = "dokarkiv.base_url".prop()

    val norg2Url = "norg2_url".prop()

    val oppgavebehandlingUrl = "oppgavebehandling.url".prop()

    val pdlUrl = "pdl_url".prop()

    private fun String.prop(): String =
        config.property(this).getString()
}

enum class AppEnv {
    PROD,
    PREPROD,
    LOCAL
}
