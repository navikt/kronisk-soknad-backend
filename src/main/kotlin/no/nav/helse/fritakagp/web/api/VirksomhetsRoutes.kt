package no.nav.helse.fritakagp.web.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.arbeidsgiver.web.auth.AltinnAuthorizer
import no.nav.helse.arbeidsgiver.web.validation.OrganisasjonsnummerValidator
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.web.auth.authorize

data class VirksomhetResponse(
    val gravidKrav: List<GravidKrav?>,
    val kroniskKrav: List<KroniskKrav?>
)

fun Route.virksomhetsRoutes(
    gravidKravRepo: GravidKravRepository,
    kroniskKravRepository: KroniskKravRepository,
    authorizer: AltinnAuthorizer
) {
    get("/virksomhet/{virksomhetsnummer}") {
        val virksomhetsnummer = requireNotNull(call.parameters["virksomhetsnummer"])
        authorize(authorizer, virksomhetsnummer)
        if (!OrganisasjonsnummerValidator.isValid(virksomhetsnummer)) {
            call.respond(HttpStatusCode.NotAcceptable, "Ikke gyldig virksomhetsnummer")
        }

        val gravidKrav = gravidKravRepo.getAllForVirksomhet(virksomhetsnummer)
        val kroniskKrav = kroniskKravRepository.getAllForVirksomhet(virksomhetsnummer)

        call.respond(HttpStatusCode.OK, VirksomhetResponse(gravidKrav = gravidKrav, kroniskKrav = kroniskKrav))
    }
}
