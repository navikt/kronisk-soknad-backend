package no.nav.helse.fritakagp.web

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.routing.*
import no.nav.helse.fritakagp.Env
import no.nav.helse.fritakagp.customObjectMapper
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
        tokenValidationSupport(config = env.config)
    }

    configureCORSAccess(env)
    configureExceptionHandling()

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(customObjectMapper()))
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    routing {
        route("${env.ktorBasepath}/api/v1") {
            authenticate {
                systemRoutes()
                kroniskRoutes(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
                gravidRoutes(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
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
