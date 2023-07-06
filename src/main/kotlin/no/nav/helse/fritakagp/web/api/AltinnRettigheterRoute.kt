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
import no.nav.helsearbeidsgiver.utils.log.logger

private val logger = "AltinnRoutes".logger()

fun Route.altinnRoutes(altinnClient: AltinnClient) {
    route("/arbeidsgivere") {
        get {
            val id = hentIdentitetsnummerFraLoginToken(call.request)
            logger.info("Henter arbeidsgivere for ${id.take(6)}")
            try {
                altinnClient.hentRettighetOrganisasjoner(id)
                    .let {
                        logger.info("Hentet rettingheter for ${id.take(6)} med ${it.size} rettigheter")
                        call.respond(it)
                    }
            } catch (_: AltinnBrukteForLangTidException) {
                call.respond(HttpStatusCode.ExpectationFailed, "Altinn brukte for lang tid på å svare, prøv igjen om litt")
            }
        }
    }
}
