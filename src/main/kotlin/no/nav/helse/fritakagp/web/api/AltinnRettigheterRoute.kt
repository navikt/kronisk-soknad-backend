package no.nav.helse.fritakagp.web.api

import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnBrukteForLangTidException
import no.nav.helse.arbeidsgiver.web.auth.AltinnOrganisationsRepository
import no.nav.helse.fritakagp.web.auth.hentIdentitetsnummerFraLoginToken
import no.nav.helsearbeidsgiver.utils.log.logger

fun Route.altinnRoutes(authRepo: AltinnOrganisationsRepository) {
    val logger = "AltinnRoutes".logger()
    route("/arbeidsgivere") {
        get("/") {
            val id = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
            logger.info("Henter arbeidsgivere for ${id.take(6)}")
            try {
                val rettigheter = authRepo.hentOrgMedRettigheterForPerson(id)
                logger.info("Hentet rettingheter for ${id.take(6)} med ${rettigheter.size} rettigheter")
                call.respond(rettigheter)
            } catch (ae: AltinnBrukteForLangTidException) {
                call.respond(HttpStatusCode.ExpectationFailed, "Altinn brukte for lang tid på å svare, prøv igjen om litt")
            }
        }
    }
}
