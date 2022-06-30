package no.nav.helse.fritakagp.web.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClient
import no.nav.helse.arbeidsgiver.web.auth.AltinnAuthorizer
import no.nav.helse.fritakagp.GravidKravMetrics
import no.nav.helse.fritakagp.GravidSoeknadMetrics
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.domain.BeløpBeregning
import no.nav.helse.fritakagp.domain.KravStatus
import no.nav.helse.fritakagp.domain.decodeBase64File
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.integration.virusscan.VirusScanner
import no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKvitteringProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.SlettGravidKravProcessor
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKvitteringProcessor
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadProcessor
import no.nav.helse.fritakagp.service.PdlService
import no.nav.helse.fritakagp.web.api.resreq.*
import no.nav.helse.fritakagp.web.auth.authorize
import no.nav.helse.fritakagp.web.auth.hentIdentitetsnummerFraLoginToken
import no.nav.helse.fritakagp.web.api.resreq.validation.VirusCheckConstraint
import no.nav.helse.fritakagp.web.api.resreq.validation.extractBase64Del
import no.nav.helse.fritakagp.web.api.resreq.validation.extractFilExtDel
import org.valiktor.ConstraintViolation
import org.valiktor.ConstraintViolationException
import org.valiktor.DefaultConstraintViolation
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

fun Route.gravidRoutes(
    breegClient: BrregClient,
    datasource: DataSource,
    gravidSoeknadRepo: GravidSoeknadRepository,
    gravidKravRepo: GravidKravRepository,
    bakgunnsjobbService: BakgrunnsjobbService,
    om: ObjectMapper,
    virusScanner: VirusScanner,
    bucket: BucketStorage,
    authorizer: AltinnAuthorizer,
    belopBeregning: BeløpBeregning,
    aaregClient: AaregArbeidsforholdClient,
    pdlService: PdlService
) {
    route("/gravid") {
        route("/soeknad") {

            get("/{id}") {
                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                val form = gravidSoeknadRepo.getById(UUID.fromString(call.parameters["id"]))
                if (form == null || form.identitetsnummer != innloggetFnr) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    form.sendtAvNavn = form.sendtAvNavn ?: pdlService.finnNavn(innloggetFnr)
                    form.navn = form.navn ?: pdlService.finnNavn(form.identitetsnummer)

                    call.respond(HttpStatusCode.OK, form)
                }
            }

            post {
                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                val request = call.receive<GravidSoknadRequest>()

                val isVirksomhet = if (application.environment.config.property("koin.profile").getString() == "PREPROD") true else breegClient.erVirksomhet(request.virksomhetsnummer)
                request.validate(isVirksomhet)

                val sendtAvNavn = pdlService.finnNavn(innloggetFnr)
                val navn = pdlService.finnNavn(request.identitetsnummer)

                val soeknad = request.toDomain(innloggetFnr, sendtAvNavn, navn)

                processDocumentForGCPStorage(request.dokumentasjon, virusScanner, bucket, soeknad.id)

                datasource.connection.use { connection ->
                    gravidSoeknadRepo.insert(soeknad, connection)
                    bakgunnsjobbService.opprettJobb<GravidSoeknadProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(GravidSoeknadProcessor.JobbData(soeknad.id)),
                        connection = connection
                    )
                    bakgunnsjobbService.opprettJobb<GravidSoeknadKvitteringProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(GravidSoeknadKvitteringProcessor.Jobbdata(soeknad.id)),
                        connection = connection
                    )
                }

                call.respond(HttpStatusCode.Created, soeknad)
                GravidSoeknadMetrics.tellMottatt()
            }
        }

        route("/krav") {
            get("/{id}") {
                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                val form = gravidKravRepo.getById(UUID.fromString(call.parameters["id"]))
                if (form == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    authorize(authorizer, form.virksomhetsnummer)
                    form.sendtAvNavn = form.sendtAvNavn ?: pdlService.finnNavn(innloggetFnr)
                    form.navn = form.navn ?: pdlService.finnNavn(form.identitetsnummer)

                    call.respond(HttpStatusCode.OK, form)
                }
            }

            post {
                val request = call.receive<GravidKravRequest>()
                authorize(authorizer, request.virksomhetsnummer)
                val arbeidsforhold = aaregClient
                    .hentArbeidsforhold(request.identitetsnummer, UUID.randomUUID().toString())
                    .filter { it.arbeidsgiver.organisasjonsnummer == request.virksomhetsnummer }

                request.validate(arbeidsforhold)

                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                val sendtAvNavn = pdlService.finnNavn(innloggetFnr)
                val navn = pdlService.finnNavn(request.identitetsnummer)

                val krav = request.toDomain(innloggetFnr, sendtAvNavn, navn)
                belopBeregning.beregnBeløpGravid(krav)
                processDocumentForGCPStorage(request.dokumentasjon, virusScanner, bucket, krav.id)

                datasource.connection.use { connection ->
                    gravidKravRepo.insert(krav, connection)
                    bakgunnsjobbService.opprettJobb<GravidKravProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(GravidKravProcessor.JobbData(krav.id)),
                        connection = connection
                    )
                    bakgunnsjobbService.opprettJobb<GravidKravKvitteringProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(GravidKravKvitteringProcessor.Jobbdata(krav.id)),
                        connection = connection
                    )
                    bakgunnsjobbService.opprettJobb<ArbeidsgiverNotifikasjonProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(ArbeidsgiverNotifikasjonProcessor.Jobbdata(krav.id, ArbeidsgiverNotifikasjonProcessor.Jobbdata.SkjemaType.KroniskKrav)),
                        connection = connection
                    )
                }

                call.respond(HttpStatusCode.Created, krav)
                GravidKravMetrics.tellMottatt()
            }

            patch("/{id}") {
                val request = call.receive<GravidKravRequest>()

                authorize(authorizer, request.virksomhetsnummer)

                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                val sendtAvNavn = pdlService.finnNavn(innloggetFnr)
                val navn = pdlService.finnNavn(request.identitetsnummer)

                val arbeidsforhold = aaregClient
                    .hentArbeidsforhold(request.identitetsnummer, UUID.randomUUID().toString())
                    .filter { it.arbeidsgiver.organisasjonsnummer == request.virksomhetsnummer }

                request.validate(arbeidsforhold)

                val kravId = UUID.fromString(call.parameters["id"])
                val kravTilSletting = gravidKravRepo.getById(kravId)
                    ?: return@patch call.respond(HttpStatusCode.NotFound)

                val kravTilOppdatering = request.toDomain(innloggetFnr, sendtAvNavn, navn)
                belopBeregning.beregnBeløpGravid(kravTilOppdatering)

                kravTilSletting.status = KravStatus.ENDRET
                kravTilSletting.slettetAv = innloggetFnr
                kravTilSletting.slettetAvNavn = sendtAvNavn
                kravTilSletting.endretDato = LocalDateTime.now()

                // Sletter gammelt krav
                datasource.connection.use { connection ->
                    gravidKravRepo.update(kravTilSletting, connection)
                    bakgunnsjobbService.opprettJobb<SlettGravidKravProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(GravidKravProcessor.JobbData(kravTilSletting.id)),
                        connection = connection
                    )
                }

                // Oppretter nytt krav
                datasource.connection.use { connection ->
                    gravidKravRepo.insert(kravTilOppdatering, connection)
                    bakgunnsjobbService.opprettJobb<GravidKravProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(GravidKravProcessor.JobbData(kravTilOppdatering.id)),
                        connection = connection
                    )
                    bakgunnsjobbService.opprettJobb<GravidKravKvitteringProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(GravidKravKvitteringProcessor.Jobbdata(kravTilOppdatering.id)),
                        connection = connection
                    )
                }

                call.respond(HttpStatusCode.OK, kravTilOppdatering)
            }

            delete("/{id}") {
                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                val slettetAv = pdlService.finnNavn(innloggetFnr)
                val kravId = UUID.fromString(call.parameters["id"])
                val form = gravidKravRepo.getById(kravId)
                    ?: return@delete call.respond(HttpStatusCode.NotFound)

                authorize(authorizer, form.virksomhetsnummer)

                form.status = KravStatus.SLETTET
                form.slettetAv = innloggetFnr
                form.slettetAvNavn = slettetAv
                form.endretDato = LocalDateTime.now()
                datasource.connection.use { connection ->
                    gravidKravRepo.update(form, connection)
                    bakgunnsjobbService.opprettJobb<SlettGravidKravProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(GravidKravProcessor.JobbData(form.id)),
                        connection = connection
                    )
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

suspend fun processDocumentForGCPStorage(doc: String?, virusScanner: VirusScanner, bucket: BucketStorage, id: UUID) {
    if (!doc.isNullOrEmpty()) {
        val fileContent = extractBase64Del(doc)
        val fileExt = extractFilExtDel(doc)
        if (!virusScanner.scanDoc(decodeBase64File(fileContent))) {
            throw ConstraintViolationException(
                setOf(
                    DefaultConstraintViolation(
                        "dokumentasjon",
                        constraint = VirusCheckConstraint()
                    )
                )
            )
        }
        bucket.uploadDoc(id, fileContent, fileExt)
    }
}
