package no.nav.helse.fritakagp.web.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.web.auth.AltinnAuthorizer
import no.nav.helse.fritakagp.KroniskKravMetrics
import no.nav.helse.fritakagp.KroniskSoeknadMetrics
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import no.nav.helse.fritakagp.domain.decodeBase64File
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringProcessor
import no.nav.helse.fritakagp.integration.virusscan.VirusScanner
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKvitteringProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravProcessor
import no.nav.helse.fritakagp.web.api.resreq.KroniskKravRequest
import no.nav.helse.fritakagp.web.api.resreq.KroniskSoknadRequest
import no.nav.helse.fritakagp.web.auth.authorize
import no.nav.helse.fritakagp.web.auth.hentIdentitetsnummerFraLoginToken
import no.nav.helse.fritakagp.web.dto.validation.extractBase64Del
import no.nav.helse.fritakagp.web.dto.validation.extractFilExtDel
import javax.sql.DataSource

@KtorExperimentalAPI
fun Route.kroniskRoutes(
    datasource: DataSource,
    kroniskSoeknadRepo: KroniskSoeknadRepository,
    kroniskKravRepo: KroniskKravRepository,
    bakgunnsjobbRepo: BakgrunnsjobbRepository,
    om: ObjectMapper,
    virusScanner: VirusScanner,
    bucket: BucketStorage,
    authorizer: AltinnAuthorizer

) {
    route("/kronisk") {
        route("/soeknad") {
            post {
                val request = call.receive<KroniskSoknadRequest>()
                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)

                val soeknad = KroniskSoeknad(
                    virksomhetsnummer = request.virksomhetsnummer,
                    identitetsnummer = request.identitetsnummer,
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
                    kroniskSoeknadRepo.insert(soeknad, connection)
                    bakgunnsjobbRepo.save(
                        Bakgrunnsjobb(
                            maksAntallForsoek = 10,
                            data = om.writeValueAsString(KroniskSoeknadProcessor.JobbData(soeknad.id)),
                            type = KroniskSoeknadProcessor.JOB_TYPE
                        ),
                        connection
                    )
                    bakgunnsjobbRepo.save(
                        Bakgrunnsjobb(
                            maksAntallForsoek = 10,
                            data = om.writeValueAsString(KroniskSoeknadKvitteringProcessor.Jobbdata(soeknad.id)),
                            type = KroniskSoeknadKvitteringProcessor.JOB_TYPE
                        ),
                        connection
                    )
                }

                call.respond(HttpStatusCode.Created)
                KroniskSoeknadMetrics.tellMottatt()
            }
        }

        route("/krav") {
            post {
                val request = call.receive<KroniskKravRequest>()
                authorize(authorizer, request.virksomhetsnummer)

                val krav = KroniskKrav(
                    identitetsnummer = request.identitetsnummer,
                    virksomhetsnummer = request.virksomhetsnummer,
                    perioder = request.perioder,
                    sendtAv = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                )

                if (!request.dokumentasjon.isNullOrEmpty()) {
                    val fileContent = extractBase64Del(request.dokumentasjon)
                    val fileExt = extractFilExtDel(request.dokumentasjon)
                    if (!virusScanner.scanDoc(decodeBase64File(fileContent))) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    bucket.uploadDoc(krav.id, fileContent, fileExt)
                }

                datasource.connection.use { connection ->
                    kroniskKravRepo.insert(krav, connection)
                    bakgunnsjobbRepo.save(
                        Bakgrunnsjobb(
                            maksAntallForsoek = 10,
                            data = om.writeValueAsString(KroniskKravProcessor.JobbData(krav.id)),
                            type = KroniskKravProcessor.JOB_TYPE
                        ),
                        connection
                    )
                    bakgunnsjobbRepo.save(
                        Bakgrunnsjobb(
                            maksAntallForsoek = 10,
                            data = om.writeValueAsString(KroniskKravKvitteringProcessor.Jobbdata(krav.id)),
                            type = KroniskKravKvitteringProcessor.JOB_TYPE
                        ),
                        connection
                    )
                }

                call.respond(HttpStatusCode.Created)
                KroniskKravMetrics.tellMottatt()
            }
        }
    }
}