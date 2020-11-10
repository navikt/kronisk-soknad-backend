package no.nav.helse.fritakagp.web.api.resreq

import java.time.LocalDate

data class GravideSoknadRequest(
    val dato: LocalDate,
    val fnr: String,
    val tilrettelegge: Boolean,
    val tiltak: String,
    val tiltakBeskrivelse: String,
    val omplassering: String
)