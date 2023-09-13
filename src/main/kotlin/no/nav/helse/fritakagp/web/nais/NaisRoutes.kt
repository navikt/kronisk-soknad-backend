package no.nav.helse.fritakagp.web.nais

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.ProbeResult
import no.nav.helse.arbeidsgiver.kubernetes.ProbeState
import org.koin.ktor.ext.get
import java.util.Collections

private val collectorRegistry = CollectorRegistry.defaultRegistry

fun Application.nais() {

    DefaultExports.initialize()

    routing {
        get("/health/is-alive") {
            val kubernetesProbeManager = this@routing.get<KubernetesProbeManager>()
            val checkResults = kubernetesProbeManager.runLivenessProbe()
            returnResultOfChecks(checkResults)
        }

        get("/health/is-ready") {
            val kubernetesProbeManager = this@routing.get<KubernetesProbeManager>()
            val checkResults = kubernetesProbeManager.runReadynessProbe()
            returnResultOfChecks(checkResults)
        }

        get("/metrics") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: Collections.emptySet()
            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
            }
        }

//        get("/healthcheck") {
//            val kubernetesProbeManager = this@routing.get<KubernetesProbeManager>()
//            val readyResults = kubernetesProbeManager.runReadynessProbe()
//            val liveResults = kubernetesProbeManager.runLivenessProbe()
//            val combinedResults = ProbeResult(
//                liveResults.healthyComponents +
//                    liveResults.unhealthyComponents +
//                    readyResults.healthyComponents +
//                    readyResults.unhealthyComponents
//            )
//
//            returnResultOfChecks(combinedResults)
//        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.returnResultOfChecks(checkResults: ProbeResult) {
    val httpResult = if (checkResults.state == ProbeState.UN_HEALTHY) HttpStatusCode.InternalServerError else HttpStatusCode.OK
    checkResults.unhealthyComponents.forEach { r ->
        r.error?.let { call.application.environment.log.error(r.toString()) }
    }
    call.respond(httpResult, checkResults)
}
