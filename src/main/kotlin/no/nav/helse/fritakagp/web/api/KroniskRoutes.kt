package no.nav.helse.fritakagp.web.api

import com.fasterxml.jackson.databind.ObjectMapper
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
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClient
import no.nav.helse.arbeidsgiver.web.auth.AltinnAuthorizer
import no.nav.helse.fritakagp.KroniskKravMetrics
import no.nav.helse.fritakagp.KroniskSoeknadMetrics
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
import no.nav.helse.fritakagp.service.PdlService
import no.nav.helse.fritakagp.web.api.resreq.KroniskKravRequest
import no.nav.helse.fritakagp.web.api.resreq.KroniskSoknadRequest
import no.nav.helse.fritakagp.web.auth.authorize
import no.nav.helse.fritakagp.web.auth.hentIdentitetsnummerFraLoginToken
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.hardDeleteSak
import no.nav.helsearbeidsgiver.utils.log.logger
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

fun Route.kroniskRoutes(
    breegClient: BrregClient,
    datasource: DataSource,
    kroniskSoeknadRepo: KroniskSoeknadRepository,
    kroniskKravRepo: KroniskKravRepository,
    bakgunnsjobbService: BakgrunnsjobbService,
    om: ObjectMapper,
    virusScanner: VirusScanner,
    bucket: BucketStorage,
    authorizer: AltinnAuthorizer,
    belopBeregning: BeloepBeregning,
    aaregClient: AaregArbeidsforholdClient,
    pdlService: PdlService,
    arbeidsgiverNotifikasjonKlient: ArbeidsgiverNotifikasjonKlient
) {
    val logger = "kroniskRoutes".logger()

    route("/kronisk") {
        route("/soeknad") {
            get("/{id}") {
                logger.info("Hent kronisk søknad.")
                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)

                logger.info("Hent søknad fra db.")
                val form = kroniskSoeknadRepo.getById(UUID.fromString(call.parameters["id"]))

                if (form == null || form.identitetsnummer != innloggetFnr) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    logger.info("Hent personinfo fra pdl.")
                    form.sendtAvNavn = form.sendtAvNavn ?: pdlService.finnNavn(innloggetFnr)
                    form.navn = form.navn ?: pdlService.finnNavn(form.identitetsnummer)

                    call.respond(HttpStatusCode.OK, form)
                }
            }

            post {
                logger.info("Send inn kronisk søknad.")
                val request = call.receive<KroniskSoknadRequest>()

                val isVirksomhet = if (application.environment.config.property("koin.profile").getString() == "PREPROD") {
                    true
                } else {
                    logger.info("Hent virksomhet fra brreg.")
                    breegClient.erVirksomhet(request.virksomhetsnummer)
                }
                request.validate(isVirksomhet)

                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)

                logger.info("Hent personinfo fra pdl.")
                val sendtAvNavn = pdlService.finnNavn(innloggetFnr)
                val navn = pdlService.finnNavn(request.identitetsnummer)

                val soeknad = request.toDomain(innloggetFnr, sendtAvNavn, navn)
                processDocumentForGCPStorage(request.dokumentasjon, virusScanner, bucket, soeknad.id)

                logger.info("Lagre søknad i db.")
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
                logger.info("Hent kronisk krav.")
                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)

                logger.info("Hent krav fra db.")
                val form = kroniskKravRepo.getById(UUID.fromString(call.parameters["id"]))

                if (form == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    if (form.identitetsnummer != innloggetFnr) {
                        authorize(authorizer, form.virksomhetsnummer)
                    }

                    logger.info("Hent personinfo fra pdl.")
                    form.sendtAvNavn = form.sendtAvNavn ?: pdlService.finnNavn(innloggetFnr)
                    form.navn = form.navn ?: pdlService.finnNavn(form.identitetsnummer)

                    call.respond(HttpStatusCode.OK, form)
                }
            }

            post {
                logger.info("Send inn kronisk krav.")

                val request = call.receive<KroniskKravRequest>()
                authorize(authorizer, request.virksomhetsnummer)

                logger.info("Hent arbeidsforhold fra aareg.")
                val arbeidsforhold = aaregClient
                    .hentArbeidsforhold(request.identitetsnummer, UUID.randomUUID().toString())
                    .filter { it.arbeidsgiver.organisasjonsnummer == request.virksomhetsnummer }

                request.validate(arbeidsforhold)

                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)

                logger.info("Hent personinfo fra pdl.")
                val sendtAvNavn = pdlService.finnNavn(innloggetFnr)
                val navn = pdlService.finnNavn(request.identitetsnummer)

                val krav = request.toDomain(innloggetFnr, sendtAvNavn, navn)

                belopBeregning.beregnBeløpKronisk(krav)
                processDocumentForGCPStorage(request.dokumentasjon, virusScanner, bucket, krav.id)

                logger.info("Legg til krav i db.")
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
                logger.info("Oppdater kronisk krav.")

                val request = call.receive<KroniskKravRequest>()

                authorize(authorizer, request.virksomhetsnummer)

                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                val sendtAvNavn = pdlService.finnNavn(innloggetFnr)
                val navn = pdlService.finnNavn(request.identitetsnummer)

                logger.info("Hent arbeidsforhold fra aareg.")
                val arbeidsforhold = aaregClient
                    .hentArbeidsforhold(request.identitetsnummer, UUID.randomUUID().toString())
                    .filter { it.arbeidsgiver.organisasjonsnummer == request.virksomhetsnummer }

                request.validate(arbeidsforhold)

                val kravId = UUID.fromString(call.parameters["id"])

                logger.info("Hent gammelt krav fra db.")
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
                logger.info("Slett sak om gammelt krav i arbeidsgivernotifikasjon.")
                kravTilSletting.arbeidsgiverSakId?.let {
                    runBlocking { arbeidsgiverNotifikasjonKlient.hardDeleteSak(it) }
                }

                logger.info("Oppdater gammelt krav til slettet i db.")
                datasource.connection.use { connection ->
                    kroniskKravRepo.update(kravTilSletting, connection)
                    bakgunnsjobbService.opprettJobb<KroniskKravSlettProcessor>(
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(KroniskKravProcessor.JobbData(kravTilSletting.id)),
                        connection = connection
                    )
                }

                // Oppretter nytt krav
                logger.info("Legg til nytt krav i db.")
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
                logger.info("Slett kronisk krav.")

                val innloggetFnr = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                val slettetAv = pdlService.finnNavn(innloggetFnr)
                val kravId = UUID.fromString(call.parameters["id"])
                val form = kroniskKravRepo.getById(kravId)
                    ?: return@delete call.respond(HttpStatusCode.NotFound)

                authorize(authorizer, form.virksomhetsnummer)

                logger.info("Slett sak i arbeidsgivernotifikasjon.")
                form.arbeidsgiverSakId?.let {
                    runBlocking { arbeidsgiverNotifikasjonKlient.hardDeleteSak(it) }
                }
                form.status = KravStatus.SLETTET
                form.slettetAv = innloggetFnr
                form.slettetAvNavn = slettetAv
                form.endretDato = LocalDateTime.now()

                logger.info("Oppdater krav til slettet i db.")
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
