package no.nav.helse.fritakagp.web.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helse.fritakagp.web.auth.hentIdentitetsnummerFraLoginToken
import no.nav.helsearbeidsgiver.altinn.AltinnBrukteForLangTidException
import no.nav.helsearbeidsgiver.altinn.AltinnClient

fun Route.altinnRoutes(altinnClient: AltinnClient) {
    route("/arbeidsgivere") {
        get {
            val id = hentIdentitetsnummerFraLoginToken(call.request)
            try {
                altinnClient.hentRettighetOrganisasjoner(id)
                    .let { call.respond(it) }
            } catch (_: AltinnBrukteForLangTidException) {
                call.respond(HttpStatusCode.ExpectationFailed, "Altinn brukte for lang tid på å svare, prøv igjen om litt")
            }
        }
    }
}
