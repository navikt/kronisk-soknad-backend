package no.nav.helse.fritakagp.web.api

import io.ktor.http.content.*
import io.ktor.routing.*

fun Route.swaggerRoutes(base: String) {

    route("$base/") {
        static("swagger") {
            default("index.html")
            resources("swagger-ui/dist")
        }
    }

    route("$base/") {
        static("docs") {
            resources("swagger-docs")
        }
    }

}
