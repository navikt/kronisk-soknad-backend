package no.nav.helse.fritakagp.config

import io.ktor.config.ApplicationConfig

enum class AppEnv {
    PROD,
    PREPROD,
    LOCAL
}

fun ApplicationConfig.env(): AppEnv =
    when (prop("koin.profile")) {
        "PROD" -> AppEnv.PROD
        "PREPROD" -> AppEnv.PREPROD
        else -> AppEnv.LOCAL
    }

fun ApplicationConfig.shouldRunBackgroundWorkers(): Boolean =
    prop("run_background_workers") == "true"

private fun ApplicationConfig.prop(key: String): String =
    property(key).getString()
