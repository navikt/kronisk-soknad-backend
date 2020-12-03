package no.nav.helse.fritakagp.web.api

import com.fasterxml.jackson.core.JsonGenerationException
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.fritakagp.db.Repository
import no.nav.helse.fritakagp.domain.*
import no.nav.helse.fritakagp.intergrasjoner.virusscan.ClamavVirusScanner
import no.nav.helse.fritakagp.intergrasjoner.virusscan.ClamavVirusScannerImp
import no.nav.helse.fritakagp.web.api.resreq.GravideSoknadRequest
import no.nav.helse.fritakagp.web.hentIdentitetsnummerFraLoginToken
import org.valiktor.validate
import java.io.File
import java.nio.file.Paths
import java.sql.SQLException

@KtorExperimentalAPI
fun Route.fritakAGP(repo: Repository) {
    route("/api/v1") {

        route("/gravid/soeknad") {
            post {
                try {
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
                        tiltakBeskrivelse = request.tiltakBeskrivelse,
                        datafil = request.datafil,
                        ext = request.ext
                    )
                    soeknad.datafil?.let {
                        val vedlagteFil: ByteArray =
                            decodeBase64File(it, request.fnr.plus("_").plus(request.dato), request.ext)
                        runBlocking {
                            if (!ClamavVirusScanner().scanFile(vedlagteFil)) {
                                call.respond(HttpStatusCode.NotAcceptable)
                            }
                        }
                    }


                    //   repo.insert(soeknad)
                } catch (ex: SQLException) {
                    call.respond(HttpStatusCode.UnprocessableEntity)
                }


                // TODO: Opprette en bakgrunnsjobb som sender søknaden videre

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
