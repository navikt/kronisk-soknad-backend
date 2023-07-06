package no.nav.helse.fritakagp.web.api

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ParameterConversionException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import no.nav.helse.arbeidsgiver.web.validation.Problem
import no.nav.helse.arbeidsgiver.web.validation.ValidationProblem
import no.nav.helse.arbeidsgiver.web.validation.ValidationProblemDetail
import no.nav.helse.fritakagp.service.ManglerAltinnRettigheterException
import no.nav.helse.fritakagp.web.dto.validation.getContextualMessage
import no.nav.helsearbeidsgiver.utils.log.logger
import org.valiktor.ConstraintViolationException
import java.lang.reflect.InvocationTargetException
import java.net.URI
import java.util.UUID

private val logger = "StatusPages".logger()

fun Application.configureExceptionHandling() {
    install(StatusPages) {

        exception(::handleUnexpectedException)
        exception(::handleBadRequestException)
        exception(::handleValidationError)
        exception(::handleMissingKotlinParameter)

        exception<ManglerAltinnRettigheterException> { call, _ ->
            call.respondProblem(
                HttpStatusCode.Forbidden,
                "Ikke korrekte Altinnrettigheter på valgt virksomhet.",
                null,
                "forbidden"
            )
        }

        exception<InvocationTargetException> { call, cause ->
            when (cause.targetException) {
                is ConstraintViolationException ->
                    handleValidationError(call, cause.targetException as ConstraintViolationException)
                else ->
                    handleUnexpectedException(call, cause)
            }
        }

        exception<JsonMappingException> { call, cause ->
            when (cause.cause) {
                is ConstraintViolationException ->
                    handleValidationError(call, cause.cause as ConstraintViolationException)
                else ->
                    handleJsonError(call, cause)
            }
        }

        exception<ParameterConversionException> { call, cause ->
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
    }
}

private suspend fun handleUnexpectedException(call: ApplicationCall, cause: Throwable) {
    val errorId = UUID.randomUUID()

    call.respondProblem(
        HttpStatusCode.InternalServerError,
        "Uventet feil.",
        cause,
        "uventet-feil",
        errorId,
    )

    val userAgent = call.request.headers[HttpHeaders.UserAgent] ?: "Ukjent"
    logger.error("Uventet feil, $errorId med useragent $userAgent", cause)
}

private suspend fun handleBadRequestException(call: ApplicationCall, cause: BadRequestException) {
    when (cause.cause) {
        // Noen JSON-feil kommer som nøstede BadRequestException
        is BadRequestException ->
            handleBadRequestException(call, cause.cause as BadRequestException)
        is JsonParseException ->
            handleJsonError(call, cause.cause as JsonParseException)
        is InvalidFormatException ->
            handleJsonError(call, cause.cause as InvalidFormatException)
        is MissingKotlinParameterException ->
            handleMissingKotlinParameter(call, cause.cause as MissingKotlinParameterException)
        else ->
            handleUnexpectedException(call, cause)
    }
}

private suspend fun <T : Throwable> handleJsonError(call: ApplicationCall, cause: T) {
    val errorType = cause.nameKebabCase() ?: "json-ukjent"
    val errorId = UUID.randomUUID()

    call.respondProblem(
        HttpStatusCode.BadRequest,
        "Feil ved prosessering av mottatt JSON-data.",
        cause,
        errorType,
        errorId,
    )

    val userAgent = call.request.headers[HttpHeaders.UserAgent] ?: "Ukjent"
    val locale = call.request.headers[HttpHeaders.AcceptLanguage] ?: "Ukjent"
    logger.warn("$errorId : $userAgent : $locale", cause)
}

private suspend fun handleValidationError(call: ApplicationCall, cause: ConstraintViolationException) {
    val problems = cause.constraintViolations
        .map {
            ValidationProblemDetail(it.constraint.name, it.getContextualMessage(), it.property, it.value)
        }
        .toSet()

    call.respond(HttpStatusCode.UnprocessableEntity, ValidationProblem(problems))
}

private suspend fun handleMissingKotlinParameter(call: ApplicationCall, cause: MissingKotlinParameterException) {
    call.respond(
        HttpStatusCode.BadRequest,
        ValidationProblem(
            setOf(
                ValidationProblemDetail(
                    "NotNull",
                    "Det angitte feltet er påkrevd",
                    cause.path
                        .filter { it.fieldName != null }
                        .joinToString(".") { it.fieldName },
                    "null"
                )
            )
        )
    )

    val parameter = cause.parameter.name ?: "Ukjent"
    val userAgent = call.request.headers[HttpHeaders.UserAgent] ?: "Ukjent"
    logger.warn("Feil med validering av $parameter for $userAgent: ${cause.message}")
}

private suspend fun <T : Throwable> ApplicationCall.respondProblem(
    status: HttpStatusCode,
    title: String,
    cause: T?,
    errorType: String,
    errorId: UUID = UUID.randomUUID()
) {
    val problemType = "urn:fritak:$errorType"

    val problem = Problem(
        status = status.value,
        title = title,
        detail = cause?.message,
        type = URI.create(problemType),
        instance = URI.create("$problemType:$errorId"),
    )

    respond(status, problem)
}

private fun <T : Any> T.nameKebabCase(): String? =
    this::class.simpleName
        ?.toList()
        ?.joinToString("") {
            if (it.isUpperCase()) "-" + it.lowercase()
            else it.toString()
        }
        ?.removePrefix("-")
