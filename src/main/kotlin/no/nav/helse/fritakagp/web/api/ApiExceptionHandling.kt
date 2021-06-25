package no.nav.helse.fritakagp.web.api

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import no.nav.helse.arbeidsgiver.web.validation.Problem
import no.nav.helse.arbeidsgiver.web.validation.ValidationProblem
import no.nav.helse.arbeidsgiver.web.validation.ValidationProblemDetail
import no.nav.helse.fritakagp.integration.altinn.ManglerAltinnRettigheterException
import no.nav.helse.fritakagp.web.dto.validation.getContextualMessage
import org.slf4j.LoggerFactory
import org.valiktor.ConstraintViolationException
import java.lang.reflect.InvocationTargetException
import java.net.URI
import java.util.*
import javax.ws.rs.ForbiddenException

fun Application.configureExceptionHandling() {
    install(StatusPages) {
        val logger = LoggerFactory.getLogger("StatusPages")

        suspend fun handleUnexpectedException(call: ApplicationCall, cause: Throwable) {
            val errorId = UUID.randomUUID()

            val userAgent = call.request.headers.get(HttpHeaders.UserAgent) ?: "Ukjent"
            logger.error("Uventet feil, $errorId med useragent $userAgent", cause)
            val problem = Problem(
                type = URI.create("urn:fritak:uventet-feil"),
                title = "Uventet feil",
                detail = cause.message,
                instance = URI.create("urn:fritak:uventent-feil:$errorId")
            )
            call.respond(HttpStatusCode.InternalServerError, problem)
        }

        suspend fun handleValidationError(call: ApplicationCall, cause: ConstraintViolationException) {
            val problems = cause.constraintViolations.map {
                ValidationProblemDetail(it.constraint.name, it.getContextualMessage(), it.property, it.value)
            }.toSet()

            call.respond(HttpStatusCode.UnprocessableEntity, ValidationProblem(problems))
        }

        exception<InvocationTargetException> { cause ->
            when (cause.targetException) {
                is ConstraintViolationException -> handleValidationError(
                    call,
                    cause.targetException as ConstraintViolationException
                )
                else -> handleUnexpectedException(call, cause)
            }
        }

        exception<ManglerAltinnRettigheterException> {
            call.respond(
                HttpStatusCode.Forbidden,
                Problem(URI.create("urn:fritak:forbidden"), "Ikke korrekte Altinnrettigheter på valgt virksomhet", HttpStatusCode.Forbidden.value)
            )
        }

        exception<Throwable> { cause ->
            handleUnexpectedException(call, cause)
        }

        exception<ParameterConversionException> { cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ValidationProblem(
                    setOf(
                        ValidationProblemDetail(
                            "ParameterConversion",
                            "Parameteret kunne ikke  konverteres til ${cause.type}",
                            cause.parameterName,
                            null
                        )
                    )
                )
            )
            logger.warn("${cause.parameterName} kunne ikke konverteres")
        }

        exception<MissingKotlinParameterException> { cause ->
            val userAgent = call.request.headers.get(HttpHeaders.UserAgent) ?: "Ukjent"
            call.respond(
                HttpStatusCode.BadRequest,
                ValidationProblem(
                    setOf(
                        ValidationProblemDetail(
                            "NotNull",
                            "Det angitte feltet er påkrevd",
                            cause.path.filter { it.fieldName != null }.joinToString(".") {
                                it.fieldName
                            },
                            "null"
                        )
                    )
                )
            )
            logger.warn("Feil med validering av ${cause.parameter.name ?: "Ukjent"} for ${userAgent}: ${cause.message}")
        }

        exception<JsonMappingException> { cause ->
            if (cause.cause is ConstraintViolationException) {
                handleValidationError(call, cause.cause as ConstraintViolationException)
            } else {
                val errorId = UUID.randomUUID()
                val userAgent = call.request.headers.get(HttpHeaders.UserAgent) ?: "Ukjent"
                val locale = call.request.headers.get(HttpHeaders.AcceptLanguage) ?: "Ukjent"
                logger.warn("$errorId : $userAgent : $locale", cause)
                val problem = Problem(
                    status = HttpStatusCode.BadRequest.value,
                    title = "Feil ved prosessering av JSON-dataene som ble oppgitt",
                    detail = cause.message,
                    instance = URI.create("urn:fritak:json-mapping-error:$errorId")
                )
                call.respond(HttpStatusCode.BadRequest, problem)
            }
        }

        exception<ConstraintViolationException> { cause ->
            handleValidationError(call, cause)
        }
    }

}