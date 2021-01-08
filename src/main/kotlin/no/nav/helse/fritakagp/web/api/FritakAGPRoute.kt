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
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helse.fritakagp.domain.SoeknadGravid
import no.nav.helse.fritakagp.domain.SoeknadKronisk
import no.nav.helse.fritakagp.domain.decodeBase64File
import no.nav.helse.fritakagp.gcp.BucketStorage
import no.nav.helse.fritakagp.processing.gravid.SoeknadGravidProcessor
import no.nav.helse.fritakagp.processing.kvittering.KvitteringJobData
import no.nav.helse.fritakagp.processing.kvittering.KvitteringProcessor
import no.nav.helse.fritakagp.virusscan.VirusScanner
import no.nav.helse.fritakagp.web.api.resreq.GravideSoknadRequest
import no.nav.helse.fritakagp.web.api.resreq.KroniskSoknadRequest
import no.nav.helse.fritakagp.web.dto.validation.extractBase64Del
import no.nav.helse.fritakagp.web.dto.validation.extractFilExtDel
import no.nav.helse.fritakagp.web.hentIdentitetsnummerFraLoginToken
import no.nav.helse.fritakagp.web.hentUtløpsdatoFraLoginToken
import org.slf4j.LoggerFactory
import javax.sql.DataSource

@KtorExperimentalAPI
fun Route.fritakAGP(
        datasource: DataSource,
        gravidRepo: GravidSoeknadRepository,
        kroniskRepo: KroniskSoeknadRepository,
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

                if (!request.dokumentasjon.isNullOrEmpty()) {
                    val fileContent = extractBase64Del(request.dokumentasjon)
                    val fileExt = extractFilExtDel(request.dokumentasjon)
                    if (!virusScanner.scanDoc(decodeBase64File(fileContent))) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    bucket.uploadDoc(soeknad.id, fileContent, fileExt)
                }

                datasource.connection.use { connection ->
                    gravidRepo.insert(soeknad, connection)
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

        route("/kronisk/soeknad") {
            post {
                val request = call.receive<KroniskSoknadRequest>()
                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)

                val soeknad = SoeknadKronisk(
                        orgnr = request.orgnr,
                        fnr = request.fnr,
                        sendtAv = innloggetFnr,
                        arbeidstyper = request.arbeidstyper,
                        paakjenningstyper = request.paakjenningstyper,
                        paakjenningBeskrivelse = request.paakjenningBeskrivelse,
                        fravaer = request.fravaer,
                        bekreftet = request.bekreftet
                )

                if (!request.dokumentasjon.isNullOrEmpty()) {
                    val filContext = extractBase64Del(request.dokumentasjon)
                    val filExt = extractFilExtDel(request.dokumentasjon)
                    if (!virusScanner.scanDoc(decodeBase64File(filContext))) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    bucket.uploadDoc(soeknad.id, filContext, filExt)
                }

                datasource.connection.use { connection ->
                    kroniskRepo.insert(soeknad, connection)
                }

                call.respond(HttpStatusCode.Created)
            }
        }
    }
}
