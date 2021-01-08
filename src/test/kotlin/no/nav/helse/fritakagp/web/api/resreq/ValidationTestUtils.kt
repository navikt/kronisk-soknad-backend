package no.nav.helse.fritakagp.web.api.resreq

import org.assertj.core.api.Assertions
import org.valiktor.ConstraintViolationException
import kotlin.reflect.KProperty1

fun <B, A> validationShouldFailFor(field: KProperty1<B, A>, block: () -> Unit): Exception {
    val thrown = org.junit.jupiter.api.assertThrows<ConstraintViolationException>(block)
    Assertions.assertThat(thrown.constraintViolations).hasSize(1)
    Assertions.assertThat(thrown.constraintViolations.first().property).isEqualTo(field.name)
    return thrown
}
