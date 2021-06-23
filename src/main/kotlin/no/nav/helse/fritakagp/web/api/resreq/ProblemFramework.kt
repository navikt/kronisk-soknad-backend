package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import org.valiktor.ConstraintViolation
import org.valiktor.i18n.toMessage
import java.net.URI
import java.util.*


/**
 * Tilbakemeldings-standard basert på
 *
 * https://tools.ietf.org/html/rfc7807#page-5
 *
 * Hvis du trenger å gi klienter tilbakemelding som inneholder
 * mer informasjon kan du arve fra denne klassen. ValidationProblem
 * er et eksempel på dette som inneholder valideringsfeil.
 */
open class Problem(
    val type: URI = URI.create("about:blank"),
    val title: String,
    val status: Int? = 500,
    val detail: String? = null,
    val instance: URI = URI.create("about:blank")
)

/**
 * Problem extension for input-validation-feil.
 * Inneholder en liste over properties som feilet validering
 */
class ValidationProblem(
    val violations: Set<ValidationProblemDetail>
) : Problem(
    URI.create("urn:grensekomp:validation-error"),
    "Valideringen av input feilet",
    422,
    "Ett eller flere felter har feil."
)

class ValidationProblemDetail(
    val index: Int? = null,
    val period: Arbeidsgiverperiode? = null,
    val validationType: String, val message: String, val propertyPath: String, val invalidValue: Any?
)

fun ConstraintViolation.getContextualMessage(locale: Locale): String {
    return if (locale == Locale.ENGLISH) {
        getContextualMessageEN()
    } else {
        getContextualMessageNO()
    }
}

fun ConstraintViolation.getContextualMessageNO(): String {
    return when {
        (this.constraint.name == "GreaterOrEqual" && this.property.endsWith(Arbeidsgiverperiode::månedsinntekt.name)) -> "Beløpet må være et positivt tall eller null"
        (this.constraint.name == "LessOrEqual" && this.property.endsWith(Arbeidsgiverperiode::månedsinntekt.name)) -> "Beløpet er for høyt"
        (this.constraint.name == "GreaterOrEqual" && this.property.endsWith(Arbeidsgiverperiode::tom.name)) -> "Fra-dato må være før til-dato"
        (this.constraint.name == "LessOrEqual" && this.property.endsWith(Arbeidsgiverperiode::tom.name)) -> "Det kan ikke kreves refusjon for datoer fremover i tid"
        else -> this.toMessage().message
    }
}

fun ConstraintViolation.getContextualMessageEN(): String {
    return when {
        (this.constraint.name == "GreaterOrEqual" && this.property.endsWith(Arbeidsgiverperiode::månedsinntekt.name)) -> "The amount must be positive"
        (this.constraint.name == "LessOrEqual" && this.property.endsWith(Arbeidsgiverperiode::månedsinntekt.name)) -> "The amount is too large"
        (this.constraint.name == "GreaterOrEqual" && this.property.endsWith(Arbeidsgiverperiode::tom.name)) -> "From-date must be before To-date"
        (this.constraint.name == "LessOrEqual" && this.property.endsWith(Arbeidsgiverperiode::tom.name)) -> "Future dates not accepted"
        else -> this.toMessage(locale = Locale.ENGLISH).message
    }
}
