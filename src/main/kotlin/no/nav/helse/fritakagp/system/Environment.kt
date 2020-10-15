package no.nav.helse.fritakagp.system

import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.fritakagp.web.getString

enum class AppEnv {
    TEST,
    LOCAL,
    PREPROD,
    PROD
}

@KtorExperimentalAPI
fun ApplicationConfig.getEnvironment(): AppEnv {
    return AppEnv.valueOf(this.getString("koin.profile"))
}
