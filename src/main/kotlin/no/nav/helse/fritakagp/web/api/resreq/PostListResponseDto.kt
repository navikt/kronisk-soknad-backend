package no.nav.helse.fritakagp.web.api.resreq

data class PostListResponseDto(
    var status: Status,
    val validationErrors: List<ValidationProblemDetail> = emptyList(),
    val genericMessage: String? = null
) {
    enum class Status {
        OK,
        VALIDATION_ERRORS,
        GENERIC_ERROR
    }
}

