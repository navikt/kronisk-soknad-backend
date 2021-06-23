package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode

data class PostListResponseDto(
    var status: Status,
    val validationErrors: List<ValidationProblemDetail> = emptyList(),
    val genericMessage: String? = null,
    val referenceNumber: String? = null
) {
    public enum class Status {
        OK,
        VALIDATION_ERRORS,
        GENERIC_ERROR
    }
}

