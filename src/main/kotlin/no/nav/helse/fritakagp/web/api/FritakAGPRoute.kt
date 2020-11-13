package no.nav.helse.fritakagp.web.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.fritakagp.db.Repository
import no.nav.helse.fritakagp.domain.SoeknadGravid
import no.nav.helse.fritakagp.web.api.resreq.GravideSoknadRequest
import no.nav.helse.fritakagp.web.hentIdentitetsnummerFraLoginToken
import org.valiktor.validate
import java.sql.SQLException

@KtorExperimentalAPI
fun Route.fritakAGP(repo:Repository) {
    route("/api/v1") {

        route("/gravid/soeknad") {
            post {
                val request = call.receive<GravideSoknadRequest>()

                validate(request) {
                    validate(GravideSoknadRequest::fnr).isValidIdentitetsnummer()

                    // Her trengs sikkert flere valideringer etterhvert
                    validate(GravideSoknadRequest::)
                }

                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)

                val soeknad = SoeknadGravid(
                    dato = request.dato,
                    fnr = request.fnr,
                    sendtAv = innloggetFnr,
                    omplassering = request.omplassering,
                    tilrettelegge = request.tilrettelegge,
                    tiltak = request.tiltak,
                    tiltakBeskrivelse = request.tiltakBeskrivelse
                )
                try {
                    repo.insert(soeknad)
                } catch (ex: SQLException) {
                    call.respond(HttpStatusCode.UnprocessableEntity)
                }


                // TODO: Opprette en bakgrunnsjobb som sender søknaden videre

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
