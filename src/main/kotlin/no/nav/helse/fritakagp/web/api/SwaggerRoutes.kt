package no.nav.helse.fritakagp.web.api

import io.ktor.server.http.content.defaultResource
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

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
