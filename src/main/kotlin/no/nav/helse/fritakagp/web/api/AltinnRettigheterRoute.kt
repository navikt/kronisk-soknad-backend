package no.nav.helse.fritakagp.web.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helse.fritakagp.integration.altinn.AltinnRepo
import no.nav.helse.fritakagp.web.auth.hentIdentitetsnummerFraLoginToken
import no.nav.helsearbeidsgiver.altinn.AltinnBrukteForLangTidException
import no.nav.helsearbeidsgiver.utils.log.logger

fun Route.altinnRoutes(authRepo: AltinnRepo) {
    val logger = "AltinnRoutes".logger()
    route("/arbeidsgivere") {
        get("/") {
            val id = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
            if (id.isEmpty()) {
                // Denne sjekken er ikke strengt nødvendig, da tokenValidation som gjøres først, ikke skal tillate at vi kommer hit
                logger.warn("Bruker er ikke innlogget")
                call.respond(HttpStatusCode.Forbidden, "Ikke innlogget")
            }
            logger.info("Henter arbeidsgivere for ${id.take(6)}")
            try {
                val rettigheter = authRepo.hentOrgMedRettigheterForPerson(id)
                logger.info("Hentet rettigheter for ${id.take(6)} med ${rettigheter.size} rettigheter")
                call.respond(rettigheter)
            } catch (ae: AltinnBrukteForLangTidException) {
                call.respond(HttpStatusCode.ExpectationFailed, "Altinn brukte for lang tid på å svare, prøv igjen om litt")
            }
        }
    }
}
