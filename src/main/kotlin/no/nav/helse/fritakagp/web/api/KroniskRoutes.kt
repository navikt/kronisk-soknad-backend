package no.nav.helse.fritakagp.web.api

import io.ktor.application.application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClient
import no.nav.helse.arbeidsgiver.web.auth.AltinnAuthorizer
import no.nav.helse.fritakagp.KroniskKravMetrics
import no.nav.helse.fritakagp.KroniskSoeknadMetrics
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helse.fritakagp.domain.BeløpBeregning
import no.nav.helse.fritakagp.domain.KravStatus
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.processing.BakgrunnsjobbOppretter
import no.nav.helse.fritakagp.processing.GcpOpplaster
import no.nav.helse.fritakagp.service.PdlService
import no.nav.helse.fritakagp.web.api.resreq.KroniskKravRequest
import no.nav.helse.fritakagp.web.api.resreq.KroniskSoknadRequest
import no.nav.helse.fritakagp.web.auth.authorize
import no.nav.helse.fritakagp.web.auth.hentIdentitetsnummerFraLoginToken
import java.util.UUID

fun Route.kroniskSoeknadRoutes(
    breegClient: BrregClient,
    kroniskSoeknadRepo: KroniskSoeknadRepository,
    pdlService: PdlService,
    gcpOpplaster: GcpOpplaster,
    bakgrunnsjobbOppretter: BakgrunnsjobbOppretter
) {

    route("/kronisk/soeknad/{id}") {
        get {
            val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
            val form = kroniskSoeknadRepo.getById(UUID.fromString(call.parameters["id"]))
            if (form == null || form.identitetsnummer != innloggetFnr) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                form.sendtAvNavn = form.sendtAvNavn ?: pdlService.finnNavn(innloggetFnr)
                form.navn = form.navn ?: pdlService.finnNavn(form.identitetsnummer)

                call.respond(HttpStatusCode.OK, form)
            }
        }
    }
    route("/kronisk/soeknad") {
        post {
            val request = call.receive<KroniskSoknadRequest>()
            val erIPreprod = application.environment.config.property("koin.profile").getString() == "PREPROD"
            val isVirksomhet = erIPreprod || breegClient.erVirksomhet(request.virksomhetsnummer)
            request.validate(isVirksomhet)

            val isAktivVirksomhet = breegClient.erAktiv(request.virksomhetsnummer)
            request.validate(isAktivVirksomhet)

            val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)

            val sendtAvNavn = pdlService.finnNavn(innloggetFnr)
            val navn = pdlService.finnNavn(request.identitetsnummer)

            val soeknad = request.toDomain(innloggetFnr, sendtAvNavn, navn)
            gcpOpplaster.processDocumentForGCPStorage(request.dokumentasjon, soeknad.id)
            bakgrunnsjobbOppretter.kroniskSoeknadBakgrunnsjobb(soeknad)
            bakgrunnsjobbOppretter.kroniskSoeknadKvitteringBakgrunnsjobb(soeknad)
            call.respond(HttpStatusCode.Created, soeknad)
            KroniskSoeknadMetrics.tellMottatt()
        }
    }
}
fun Route.kroniskKravRoutes(
    kroniskKravRepo: KroniskKravRepository,
    authorizer: AltinnAuthorizer,
    belopBeregning: BeløpBeregning,
    aaregClient: AaregArbeidsforholdClient,
    pdlService: PdlService,
    gcpOpplaster: GcpOpplaster,
    bakgrunnsjobbOppretter: BakgrunnsjobbOppretter
) {
    route("/kronisk/krav/virksomhet/{virksomhetsnummer}") {
        get {
            val virksomhetsnummer = requireNotNull(call.parameters["virksomhetsnummer"])
            authorize(authorizer, virksomhetsnummer)

            val kroniskKrav = kroniskKravRepo.getAllForVirksomhet(virksomhetsnummer)

            call.respond(HttpStatusCode.OK, kroniskKrav)
        }
    }
    route("/kronisk/krav/{id}") {
        get {
            val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
            val form = kroniskKravRepo.getById(UUID.fromString(call.parameters["id"]))
            if (form == null || form.identitetsnummer != innloggetFnr) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                form.sendtAvNavn = form.sendtAvNavn ?: pdlService.finnNavn(innloggetFnr)
                form.navn = form.navn ?: pdlService.finnNavn(form.identitetsnummer)

                call.respond(HttpStatusCode.OK, form)
            }
        }
        delete {
            val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
            val slettetAv = pdlService.finnNavn(innloggetFnr)
            val kravId = UUID.fromString(call.parameters["id"])
            val form = kroniskKravRepo.getById(kravId)
            if (form == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                authorize(authorizer, form.virksomhetsnummer)
                bakgrunnsjobbOppretter.kroniskKravEndretBakgrunnsjobb(KravStatus.SLETTET, innloggetFnr, slettetAv, form)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
    route("/kronisk/krav") {
        post {
            val request = call.receive<KroniskKravRequest>()
            authorize(authorizer, request.virksomhetsnummer)
            val arbeidsforhold = aaregClient
                .hentArbeidsforhold(request.identitetsnummer, UUID.randomUUID().toString())
                .filter { it.arbeidsgiver.organisasjonsnummer == request.virksomhetsnummer }

            request.validate(arbeidsforhold)

            val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
            val sendtAvNavn = pdlService.finnNavn(innloggetFnr)
            val navn = pdlService.finnNavn(request.identitetsnummer)

            val krav = request.toDomain(innloggetFnr, sendtAvNavn, navn)

            belopBeregning.beregnBeløpKronisk(krav)
            gcpOpplaster.processDocumentForGCPStorage(request.dokumentasjon, krav.id)
            bakgrunnsjobbOppretter.kroniskKravBakgrunnsjobb(krav)
            bakgrunnsjobbOppretter.kroniskKravKvitteringBakgrunnsjobb(krav)
            call.respond(HttpStatusCode.Created, krav)
            KroniskKravMetrics.tellMottatt()
        }
    }
}
