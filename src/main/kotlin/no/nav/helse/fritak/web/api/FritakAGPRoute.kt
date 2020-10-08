package no.nav.helse.fritak.web.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*

@KtorExperimentalAPI
fun Route.fritakAGP() {
    route("/") {
        get {
            call.respond(HttpStatusCode.OK, "OK")
        }
    }

}
