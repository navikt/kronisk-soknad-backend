package no.nav.helse.fritakagp.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.IgnoreTrailingSlash
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.fritakagp.Env
import no.nav.helse.fritakagp.web.api.altinnRoutes
import no.nav.helse.fritakagp.web.api.configureExceptionHandling
import no.nav.helse.fritakagp.web.api.gravidRoutes
import no.nav.helse.fritakagp.web.api.kroniskRoutes
import no.nav.helse.fritakagp.web.api.swaggerRoutes
import no.nav.helse.fritakagp.web.api.systemRoutes
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.koin.ktor.ext.get

@OptIn(KtorExperimentalAPI::class)
fun Application.fritakModule(env: Env) {

    install(IgnoreTrailingSlash)
    install(Authentication) {
        tokenValidationSupport(config = environment.config)
    }

    configureCORSAccess(env)
    configureExceptionHandling()

    install(ContentNegotiation) {
        val commonObjectMapper = get<ObjectMapper>()
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
        method(HttpMethod.Options)
        method(HttpMethod.Post)
        method(HttpMethod.Get)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)

        when (env) {
            is Env.Prod -> host("arbeidsgiver.nav.no", schemes = listOf("https"))
            is Env.Preprod -> host("arbeidsgiver.intern.dev.nav.no", schemes = listOf("https"))
            is Env.Local -> anyHost()
        }

        allowCredentials = true
        allowNonSimpleContentTypes = true
    }
}
