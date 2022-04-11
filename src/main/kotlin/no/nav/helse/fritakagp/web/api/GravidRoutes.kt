package no.nav.helse.fritakagp.web.api

import io.ktor.application.application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClient
import no.nav.helse.arbeidsgiver.web.auth.AltinnAuthorizer
import no.nav.helse.fritakagp.GravidKravMetrics
import no.nav.helse.fritakagp.GravidSoeknadMetrics
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.domain.BeløpBeregning
import no.nav.helse.fritakagp.domain.KravStatus
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.processing.BakgrunnsjobbProcessor
import no.nav.helse.fritakagp.processing.GcpOpplasting
import no.nav.helse.fritakagp.service.PdlService
import no.nav.helse.fritakagp.web.api.resreq.GravidKravRequest
import no.nav.helse.fritakagp.web.api.resreq.GravidSoknadRequest
import no.nav.helse.fritakagp.web.api.resreq.ValidationProblemDetail
import no.nav.helse.fritakagp.web.api.resreq.getContextualMessageNO
import no.nav.helse.fritakagp.web.auth.authorize
import no.nav.helse.fritakagp.web.auth.hentIdentitetsnummerFraLoginToken
import org.valiktor.ConstraintViolation
import java.util.UUID

fun Route.gravidSoeknadRoutes(
    breegClient: BrregClient,
    gravidSoeknadRepo: GravidSoeknadRepository,
    bakgrunnsjobbProcessor: BakgrunnsjobbProcessor,
    gcpOpplasting: GcpOpplasting,
    pdlService: PdlService
) {
    route("/gravid/soeknad/{id}") {
        get {
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
    }
    route("/gravid/soeknad/") {
        post {
            val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
            val request = call.receive<GravidSoknadRequest>()

//            var isVirksomhet = false
//            if (application.environment.config.property("koin.profile").getString() == "PREPROD")
//                isVirksomhet = true
//            else breegClient.erVirksomhet(request.virksomhetsnummer)
//            request.validate(isVirksomhet)
            val isVirksomhet = if (application.environment.config.property("koin.profile").getString() == "PREPROD") true else breegClient.erVirksomhet(request.virksomhetsnummer)
            request.validate(isVirksomhet)
            val isAktivVirksomhet = breegClient.erAktiv(request.virksomhetsnummer)
            request.validate(isAktivVirksomhet)

            val sendtAvNavn = pdlService.finnNavn(innloggetFnr)
            val navn = pdlService.finnNavn(request.identitetsnummer)

            val soeknad = request.toDomain(innloggetFnr, sendtAvNavn, navn)
            gcpOpplasting.processDocumentForGCPStorage(request.dokumentasjon, soeknad.id)
            bakgrunnsjobbProcessor.gravidSoeknadBakgrunnsjobb(soeknad)
            bakgrunnsjobbProcessor.gravidSoeknadKvitteringBakgrunnsjobb(soeknad)

            call.respond(HttpStatusCode.Created, soeknad)
            GravidSoeknadMetrics.tellMottatt()
        }
    }
}
fun Route.gravidKravRoutes(
    gravidKravRepo: GravidKravRepository,
    bakgrunnsjobbProcessor: BakgrunnsjobbProcessor,
    gcpOpplasting: GcpOpplasting,
    authorizer: AltinnAuthorizer,
    belopBeregning: BeløpBeregning,
    aaregClient: AaregArbeidsforholdClient,
    pdlService: PdlService
) {
    route("/gravid/krav/virksomhet/{virksomhetsnummer}") {
        get {
            val virksomhetsnummer = requireNotNull(call.parameters["virksomhetsnummer"])
            authorize(authorizer, virksomhetsnummer)

            val gravidKrav = gravidKravRepo.getAllForVirksomhet(virksomhetsnummer)

            call.respond(HttpStatusCode.OK, gravidKrav)
        }
    }
    route("/gravid/krav/{id}") {
        get {
            val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
            val form = gravidKravRepo.getById(UUID.fromString(call.parameters["id"]))
            if (form == null || form.identitetsnummer != innloggetFnr) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                form.sendtAvNavn = form.sendtAvNavn ?: pdlService.finnNavn(innloggetFnr)
                form.navn = form.navn ?: pdlService.finnNavn(form.identitetsnummer)

                call.respond(HttpStatusCode.OK, form)
            }
        }
        patch {
            val request = call.receive<GravidKravRequest>()
            authorize(authorizer, request.virksomhetsnummer)

            val arbeidsforhold = aaregClient
                .hentArbeidsforhold(request.identitetsnummer, UUID.randomUUID().toString())
                .filter { it.arbeidsgiver.organisasjonsnummer == request.virksomhetsnummer }

            request.validate(arbeidsforhold)

            val kravId = UUID.fromString(call.parameters["id"])
            var kravTilSletting = gravidKravRepo.getById(kravId)
                ?: return@patch call.respond(HttpStatusCode.NotFound)
            val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
            val sendtAvNavn = pdlService.finnNavn(innloggetFnr)
            val navn = pdlService.finnNavn(request.identitetsnummer)
            bakgrunnsjobbProcessor.gravidKravEndretBakgrunnsjobb(KravStatus.ENDRET, innloggetFnr, navn, kravTilSletting)

            val kravTilOppdatering = request.toDomain(innloggetFnr, sendtAvNavn, navn)
            belopBeregning.beregnBeløpGravid(kravTilOppdatering)
            bakgrunnsjobbProcessor.gravidKravBakgrunnsjobb(kravTilOppdatering)
            bakgrunnsjobbProcessor.gravidKravKvitteringBakgrunnsjobb(kravTilOppdatering)
            call.respond(HttpStatusCode.OK, kravTilOppdatering)
        }
        delete {
            val kravId = UUID.fromString(call.parameters["id"])
            var form = gravidKravRepo.getById(kravId)
                ?: return@delete call.respond(HttpStatusCode.NotFound)

            authorize(authorizer, form.virksomhetsnummer)
            val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
            val slettetAv = pdlService.finnNavn(innloggetFnr)
            bakgrunnsjobbProcessor.gravidKravEndretBakgrunnsjobb(KravStatus.SLETTET, innloggetFnr, slettetAv, form)
            call.respond(HttpStatusCode.OK)
        }
    }
    route("/gravid/krav/") {
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
            gcpOpplasting.processDocumentForGCPStorage(request.dokumentasjon, krav.id)
            bakgrunnsjobbProcessor.gravidKravBakgrunnsjobb(krav)
            bakgrunnsjobbProcessor.gravidKravKvitteringBakgrunnsjobb(krav)
            call.respond(HttpStatusCode.Created, krav)
            GravidKravMetrics.tellMottatt()
        }
    }
}

fun periodValErrs(it: ConstraintViolation): List<ValidationProblemDetail> {
    val valErrs = mutableListOf<ValidationProblemDetail>()
    if (it.property == "perioder") {
        (it.value as Set<*>).forEach { _ ->
            valErrs.add(
                ValidationProblemDetail(
                    it.constraint.name,
                    it.getContextualMessageNO(),
                    it.property,
                    it.value
                )
            )
        }
    } else {
        valErrs.add(
            ValidationProblemDetail(
                it.constraint.name,
                it.getContextualMessageNO(),
                it.property,
                it.value
            )
        )
    }

    return valErrs
}
