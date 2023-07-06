package no.nav.helse.fritakagp.config

import io.ktor.server.config.ApplicationConfig

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

fun ApplicationConfig.jdbcUrl(): String =
    "jdbc:postgresql://${prop("database.host")}:${prop("database.port")}/${prop("database.name")}"

fun ApplicationConfig.shouldRunBackgroundWorkers(): Boolean =
    prop("run_background_workers") == "true"

fun ApplicationConfig.jwtIssuerCookieName(): String =
    jwtIssuer().prop("cookie_name")

fun ApplicationConfig.jwtIssuerName(): String =
    jwtIssuer().prop("issuer_name")

fun ApplicationConfig.jwtIssuerAudience(): String =
    jwtIssuer().prop("accepted_audience")

fun ApplicationConfig.prop(key: String): String =
    property(key).getString()

private fun ApplicationConfig.jwtIssuer(): ApplicationConfig =
    configList("no.nav.security.jwt.issuers")
        .first()
