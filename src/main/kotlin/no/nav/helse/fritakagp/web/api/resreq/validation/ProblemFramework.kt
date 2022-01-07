package no.nav.helse.fritakagp.web.dto.validation

import org.valiktor.ConstraintViolation
import org.valiktor.i18n.toMessage

fun ConstraintViolation.getContextualMessage(): String {
    return this.toMessage().message
}
