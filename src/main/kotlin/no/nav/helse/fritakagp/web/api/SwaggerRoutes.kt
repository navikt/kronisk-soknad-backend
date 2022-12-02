package no.nav.helse.fritakagp.web.api

import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.Route
import io.ktor.routing.route

fun Route.swaggerRoutes(base: String) {

    route("$base") {
        static("swagger") {
            defaultResource("swagger-ui/dist/index.html")
            resources("swagger-ui/dist")
        }

        static("docs") {
            resources("swagger-docs")
        }
    }
}
