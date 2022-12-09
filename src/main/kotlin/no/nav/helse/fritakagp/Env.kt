package no.nav.helse.fritakagp

import io.ktor.config.ApplicationConfig

class Env(val config: ApplicationConfig) {
    val appEnv = when ("koin.profile".prop()) {
        "PROD" -> AppEnv.PROD
        "PREPROD" -> AppEnv.PREPROD
        else -> AppEnv.LOCAL
    }
    val appShouldRunBackgroundWorkers = "run_background_workers".prop() == "true"

    val databaseUrl = "jdbc:postgresql://%s:%s/%s".format("database.host".prop(), "database.port".prop(), "database.name".prop())
    val databaseUsername = "database.username".prop()
    val databasePassword = "database.password".prop()

    val altinnEndpoint = "altinn_melding.altinn_endpoint".prop()
    val altinnServiceId = "altinn_melding.service_id".prop()
    val altinnUsername = "altinn_melding.username".prop()
    val altinnPassword = "altinn_melding.password".prop()

    val frontendUrl = "frontend_app_url".prop()

    val datapakkeUrl = "datapakke.api_url".prop()
    val datapakkeId = "datapakke.id".prop()

    private fun String.prop(): String =
        config.prop(this)
}

enum class AppEnv {
    PROD,
    PREPROD,
    LOCAL
}

fun ApplicationConfig.prop(key: String): String =
    property(key).getString()
