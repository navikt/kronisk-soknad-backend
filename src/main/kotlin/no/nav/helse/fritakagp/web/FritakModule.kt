package no.nav.helse.fritakagp.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.helse.fritakagp.Env
import no.nav.helse.fritakagp.web.api.altinnRoutes
import no.nav.helse.fritakagp.web.api.configureExceptionHandling
import no.nav.helse.fritakagp.web.api.gravidRoutes
import no.nav.helse.fritakagp.web.api.kroniskRoutes
import no.nav.helse.fritakagp.web.api.swaggerRoutes
import no.nav.helse.fritakagp.web.api.systemRoutes
import no.nav.security.token.support.v2.tokenValidationSupport
import org.koin.ktor.ext.get

fun Application.fritakModule(env: Env) {

    install(IgnoreTrailingSlash)
    install(Authentication) {
        tokenValidationSupport(config = this@fritakModule.environment.config)
    }

    configureCORSAccess(env)
    configureExceptionHandling()

    install(ContentNegotiation) {
        val commonObjectMapper = this@fritakModule.get<ObjectMapper>()
        register(ContentType.Application.Json, JacksonConverter(commonObjectMapper))
    }

    routing {
        route("${env.ktorBasepath}/api/v1") {
            authenticate {
                systemRoutes()
                kroniskRoutes(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
                gravidRoutes(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
                altinnRoutes(get())
            }
        }
        swaggerRoutes(env.ktorBasepath)
    }
}

private fun Application.configureCORSAccess(env: Env) {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)

        when (env) {
            is Env.Prod -> allowHost("arbeidsgiver.nav.no", schemes = listOf("https"))
            is Env.Preprod -> allowHost("arbeidsgiver.intern.dev.nav.no", schemes = listOf("https"))
            is Env.Local -> anyHost()
        }

        allowCredentials = true
        allowNonSimpleContentTypes = true
    }
}
