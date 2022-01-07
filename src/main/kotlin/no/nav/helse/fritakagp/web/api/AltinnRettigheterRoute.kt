package no.nav.helse.fritakagp.web.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnBrukteForLangTidException
import no.nav.helse.arbeidsgiver.web.auth.AltinnOrganisationsRepository
import no.nav.helse.fritakagp.web.auth.hentIdentitetsnummerFraLoginToken

fun Route.altinnRoutes(authRepo: AltinnOrganisationsRepository) {
     route("/arbeidsgivere") {
        get("/") {
            val id = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
            try {
                val rettigheter = authRepo.hentOrgMedRettigheterForPerson(id)
                call.respond(rettigheter)
            } catch (ae: AltinnBrukteForLangTidException) {
                call.respond(HttpStatusCode.ExpectationFailed, "Altinn brukte for lang tid på å svare, prøv igjen om litt")
            }
        }
    }
}
