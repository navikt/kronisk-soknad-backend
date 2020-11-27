package no.nav.helse.fritakagp.web.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import no.nav.helse.fritakagp.db.Repository
import no.nav.helse.fritakagp.domain.SoeknadGravid
import no.nav.helse.fritakagp.domain.getTiltakValue
import no.nav.helse.fritakagp.web.api.resreq.GravideSoknadRequest
import no.nav.helse.fritakagp.web.hentIdentitetsnummerFraLoginToken
import no.nav.helse.fritakagp.web.hentUtløpsdatoFraLoginToken
import java.sql.SQLException

@KtorExperimentalAPI
fun Route.fritakAGP(repo:Repository) {

    route("/api/v1") {

        route("/login-expiry") {
            get {
                call.respond(HttpStatusCode.OK, hentUtløpsdatoFraLoginToken(application.environment.config, call.request))
            }
        }

        route("/gravid/soeknad") {
            post {
                val request = call.receive<GravideSoknadRequest>()
                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)

                val soeknad = SoeknadGravid(
                        dato = request.dato,
                        fnr = request.fnr,
                        sendtAv = innloggetFnr,
                        omplassering = request.omplassering,
                        omplasseringAarsak = request.omplasseringAarsak,
                        tilrettelegge = request.tilrettelegge,
                        tiltak = getTiltakValue(request.tiltak),
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
