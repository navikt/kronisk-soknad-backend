package no.nav.helse.fritakagp.web.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClient
import no.nav.helse.fritakagp.KroniskKravMetrics
import no.nav.helse.fritakagp.KroniskSoeknadMetrics
import no.nav.helse.fritakagp.config.AppEnv
import no.nav.helse.fritakagp.config.env
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helse.fritakagp.domain.BeloepBeregning
import no.nav.helse.fritakagp.domain.KravStatus
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.integration.virusscan.VirusScanner
import no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKvitteringProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravSlettProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadProcessor
import no.nav.helse.fritakagp.service.AltinnService
import no.nav.helse.fritakagp.service.PdlService
import no.nav.helse.fritakagp.web.api.resreq.KroniskKravRequest
import no.nav.helse.fritakagp.web.api.resreq.KroniskSoknadRequest
import no.nav.helse.fritakagp.web.auth.hentIdentitetsnummerFraLoginToken
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

fun Route.kroniskRoutes(
    altinnService: AltinnService,
    breegClient: BrregClient,
    datasource: DataSource,
    kroniskSoeknadRepo: KroniskSoeknadRepository,
    kroniskKravRepo: KroniskKravRepository,
    bakgunnsjobbService: BakgrunnsjobbService,
    om: ObjectMapper,
    virusScanner: VirusScanner,
    bucket: BucketStorage,
    belopBeregning: BeloepBeregning,
    aaregClient: AaregArbeidsforholdClient,
    pdlService: PdlService
) {
    route("/kronisk") {
        route("/soeknad") {
            get("/{id}") {
                val innloggetFnr = hentIdentitetsnummerFraLoginToken(call.request)
                val form = kroniskSoeknadRepo.getById(UUID.fromString(call.parameters["id"]))
                if (form == null || form.identitetsnummer != innloggetFnr) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    form.sendtAvNavn = form.sendtAvNavn ?: pdlService.finnNavn(innloggetFnr)
                    form.navn = form.navn ?: pdlService.finnNavn(form.identitetsnummer)

                    call.respond(HttpStatusCode.OK, form)
                }
            }

            post {
                val request = call.receive<KroniskSoknadRequest>()

                val isVirksomhet = if (application.environment.config.env() == AppEnv.PREPROD) true else breegClient.erVirksomhet(request.virksomhetsnummer)
                request.validate(isVirksomhet)

                val innloggetFnr = hentIdentitetsnummerFraLoginToken(call.request)

                val sendtAvNavn = pdlService.finnNavn(innloggetFnr)
                val navn = pdlService.finnNavn(request.identitetsnummer)

                val soeknad = request.toDomain(innloggetFnr, sendtAvNavn, navn)
                processDocumentForGCPStorage(request.dokumentasjon, virusScanner, bucket, soeknad.id)

                datasource.connection.use { connection ->
                    kroniskSoeknadRepo.insert(soeknad, connection)
                    bakgunnsjobbService.opprettJobb<KroniskSoeknadProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(KroniskSoeknadProcessor.JobbData(soeknad.id)),
                        connection = connection
                    )
                    bakgunnsjobbService.opprettJobb<KroniskSoeknadKvitteringProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(KroniskSoeknadKvitteringProcessor.Jobbdata(soeknad.id)),
                        connection = connection
                    )
                }

                call.respond(HttpStatusCode.Created, soeknad)
                KroniskSoeknadMetrics.tellMottatt()
            }
        }

        route("/krav") {
            get("/{id}") {
                val innloggetFnr = hentIdentitetsnummerFraLoginToken(call.request)
                val form = kroniskKravRepo.getById(UUID.fromString(call.parameters["id"]))
                if (form == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    altinnService.authorize(this, form.virksomhetsnummer)
                    form.sendtAvNavn = form.sendtAvNavn ?: pdlService.finnNavn(innloggetFnr)
                    form.navn = form.navn ?: pdlService.finnNavn(form.identitetsnummer)

                    call.respond(HttpStatusCode.OK, form)
                }
            }

            post {
                val request = call.receive<KroniskKravRequest>()
                altinnService.authorize(this, request.virksomhetsnummer)
                val arbeidsforhold = aaregClient
                    .hentArbeidsforhold(request.identitetsnummer, UUID.randomUUID().toString())
                    .filter { it.arbeidsgiver.organisasjonsnummer == request.virksomhetsnummer }

                request.validate(arbeidsforhold)

                val innloggetFnr = hentIdentitetsnummerFraLoginToken(call.request)
                val sendtAvNavn = pdlService.finnNavn(innloggetFnr)
                val navn = pdlService.finnNavn(request.identitetsnummer)

                val krav = request.toDomain(innloggetFnr, sendtAvNavn, navn)

                belopBeregning.beregnBeløpKronisk(krav)
                processDocumentForGCPStorage(request.dokumentasjon, virusScanner, bucket, krav.id)

                datasource.connection.use { connection ->
                    kroniskKravRepo.insert(krav, connection)
                    bakgunnsjobbService.opprettJobb<KroniskKravProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(KroniskKravProcessor.JobbData(krav.id)),
                        connection = connection
                    )
                    bakgunnsjobbService.opprettJobb<KroniskKravKvitteringProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(KroniskKravKvitteringProcessor.Jobbdata(krav.id)),
                        connection = connection
                    )
                    bakgunnsjobbService.opprettJobb<ArbeidsgiverNotifikasjonProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(ArbeidsgiverNotifikasjonProcessor.JobbData(krav.id, ArbeidsgiverNotifikasjonProcessor.JobbData.SkjemaType.KroniskKrav)),
                        connection = connection
                    )
                }

                call.respond(HttpStatusCode.Created, krav)
                KroniskKravMetrics.tellMottatt()
            }

            patch("/{id}") {
                val request = call.receive<KroniskKravRequest>()

                altinnService.authorize(this, request.virksomhetsnummer)

                val innloggetFnr = hentIdentitetsnummerFraLoginToken(call.request)
                val sendtAvNavn = pdlService.finnNavn(innloggetFnr)
                val navn = pdlService.finnNavn(request.identitetsnummer)

                val arbeidsforhold = aaregClient
                    .hentArbeidsforhold(request.identitetsnummer, UUID.randomUUID().toString())
                    .filter { it.arbeidsgiver.organisasjonsnummer == request.virksomhetsnummer }

                request.validate(arbeidsforhold)

                val kravId = UUID.fromString(call.parameters["id"])
                val kravTilSletting = kroniskKravRepo.getById(kravId)
                    ?: return@patch call.respond(HttpStatusCode.NotFound)

                if (kravTilSletting.virksomhetsnummer != request.virksomhetsnummer) {
                    return@patch call.respond(HttpStatusCode.Forbidden)
                }

                val kravTilOppdatering = request.toDomain(innloggetFnr, sendtAvNavn, navn)
                belopBeregning.beregnBeløpKronisk(kravTilOppdatering)

                kravTilSletting.status = KravStatus.ENDRET
                kravTilSletting.slettetAv = innloggetFnr
                kravTilSletting.slettetAvNavn = sendtAvNavn
                kravTilSletting.endretDato = LocalDateTime.now()

                // Sletter gammelt krav
                datasource.connection.use { connection ->
                    kroniskKravRepo.update(kravTilSletting, connection)
                    bakgunnsjobbService.opprettJobb<KroniskKravSlettProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(KroniskKravProcessor.JobbData(kravTilSletting.id)),
                        connection = connection
                    )
                }

                // Oppretter nytt krav
                datasource.connection.use { connection ->
                    kroniskKravRepo.insert(kravTilOppdatering, connection)
                    bakgunnsjobbService.opprettJobb<KroniskKravProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(KroniskKravProcessor.JobbData(kravTilOppdatering.id)),
                        connection = connection
                    )
                    bakgunnsjobbService.opprettJobb<KroniskKravKvitteringProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(KroniskKravKvitteringProcessor.Jobbdata(kravTilOppdatering.id)),
                        connection = connection
                    )
                    bakgunnsjobbService.opprettJobb<ArbeidsgiverNotifikasjonProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(ArbeidsgiverNotifikasjonProcessor.JobbData(kravTilOppdatering.id, ArbeidsgiverNotifikasjonProcessor.JobbData.SkjemaType.KroniskKrav)),
                        connection = connection
                    )
                }

                call.respond(HttpStatusCode.OK, kravTilOppdatering)
            }

            delete("/{id}") {
                val innloggetFnr = hentIdentitetsnummerFraLoginToken(call.request)
                val slettetAv = pdlService.finnNavn(innloggetFnr)
                val kravId = UUID.fromString(call.parameters["id"])
                var form = kroniskKravRepo.getById(kravId)
                if (form == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    altinnService.authorize(this, form.virksomhetsnummer)
                    form.status = KravStatus.SLETTET
                    form.slettetAv = innloggetFnr
                    form.slettetAvNavn = slettetAv
                    form.endretDato = LocalDateTime.now()
                    datasource.connection.use { connection ->
                        kroniskKravRepo.update(form, connection)
                        bakgunnsjobbService.opprettJobb<KroniskKravSlettProcessor>(
                            maksAntallForsoek = 10,
                            data = om.writeValueAsString(KroniskKravProcessor.JobbData(form.id)),
                            connection = connection
                        )
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
