package no.nav.helse.fritakagp.web.nais

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import java.util.Collections

private val collectorRegistry = CollectorRegistry.defaultRegistry

fun Application.nais() {
    DefaultExports.initialize()

    routing {
        get("/health/alive") {
            call.respond(HttpStatusCode(200, "OK"))
        }

        get("/health/ready") {
            call.respond(HttpStatusCode(200, "OK"))
        }

        get("/metrics") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: Collections.emptySet()
            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
            }
        }

        get("/healthcheck") {
            call.respond(HttpStatusCode(200, "OK"))
        }
    }
}
