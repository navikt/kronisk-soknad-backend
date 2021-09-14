package no.nav.helse.fritakagp.domain

import no.nav.helse.fritakagp.db.SimpleJsonbEntity
import java.time.LocalDateTime
import java.util.*

data class GravidKrav(
    override val id: UUID = UUID.randomUUID(),
    val opprettet: LocalDateTime = LocalDateTime.now(),
    var sendtAv: String,

    val virksomhetsnummer: String,
    val identitetsnummer: String,
    val perioder: List<Arbeidsgiverperiode>,
    val harVedlegg: Boolean = false,
    val kontrollDager: Int?,
    val antallDager: Int,

    var journalpostId: String? = null,

    var oppgaveId: String? = null,
    var virksomhetsnavn: String? = null,
    // Må være null for tidligere verdier er lagret med null
    val sendtAvNavn: String? = null
) : SimpleJsonbEntity