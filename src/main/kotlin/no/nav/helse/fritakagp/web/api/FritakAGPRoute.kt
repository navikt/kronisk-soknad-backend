package no.nav.helse.fritakagp.web.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.domain.SoeknadGravid
import no.nav.helse.fritakagp.domain.decodeBase64File
import no.nav.helse.fritakagp.gcp.BucketStorage
import no.nav.helse.fritakagp.processing.gravid.SoeknadGravidProcessor
import no.nav.helse.fritakagp.virusscan.VirusScanner
import no.nav.helse.fritakagp.web.api.resreq.GravideSoknadRequest
import no.nav.helse.fritakagp.web.hentIdentitetsnummerFraLoginToken
import no.nav.helse.fritakagp.web.hentUtløpsdatoFraLoginToken
import org.slf4j.LoggerFactory
import javax.sql.DataSource

@KtorExperimentalAPI
fun Route.fritakAGP(
    datasource: DataSource,
    repo: GravidSoeknadRepository,
    bakgunnsjobbRepo: BakgrunnsjobbRepository,
    om: ObjectMapper,
    virusScanner: VirusScanner,
    bucket: BucketStorage

) {

    val logger = LoggerFactory.getLogger("FritakAGP API")

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
                        orgnr = request.orgnr,
                        fnr = request.fnr,
                        sendtAv = innloggetFnr,
                        omplassering = request.omplassering,
                        omplasseringAarsak = request.omplasseringAarsak,
                        tilrettelegge = request.tilrettelegge,
                        tiltak = request.tiltak,
                        tiltakBeskrivelse = request.tiltakBeskrivelse
                )
                val filContext = request.datafil
                val filExt = request.ext

                filContext?.let {
                    if (!virusScanner.scanDoc(decodeBase64File(it))) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    bucket.uploadDoc(soeknad.id, it, filExt!!)
                }

                datasource.connection.use { connection ->
                    repo.insert(soeknad, connection)
                    bakgunnsjobbRepo.save(
                        Bakgrunnsjobb(
                            maksAntallForsoek = 10,
                            data = om.writeValueAsString(SoeknadGravidProcessor.JobbData(soeknad.id)),
                            type = SoeknadGravidProcessor.JOB_TYPE),
                        connection
                    )
                    bakgunnsjobbRepo.save(
                            Bakgrunnsjobb(
                                    maksAntallForsoek = 10,
                                    data = om.writeValueAsString(KvitteringJobData(soeknad.id)),
                                    type = KvitteringProcessor.JOB_TYPE),
                            connection
                    )
                }

                call.respond(HttpStatusCode.Created)
            }
        }
    }
}
