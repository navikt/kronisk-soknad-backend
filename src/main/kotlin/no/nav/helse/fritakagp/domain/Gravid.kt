package no.nav.helse.fritakagp.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class SoeknadGravid(
        val id: UUID = UUID.randomUUID(),
        val opprettet: LocalDateTime = LocalDateTime.now(),

        val dato: LocalDate,
        val fnr: String,
        val tilrettelegge: Boolean,
        val tiltak: List<Tiltak>? = null,
        val tiltakBeskrivelse: String? = null,
        val omplassering: Omplassering? = null,
        val sendtAv: String,

        /**
         * ID fra joark etter arkivering
         */
        val journalpostId: String? = null,

        /**
         * ID fra oppgave etter opprettelse av oppgave
         */
        val oppgaveId: String? = null
)

enum class Tiltak {TILPASSET_ARBEIDSTID, HJEMMEKONTOR, TILPASSEDE_ARBEIDSOPPGAVER, ANNET }
enum class Omplassering {JA, NEI, MOTSETTER, FAAR_IKKE_KONTAKT, IKKE_ANDRE_OPPGAVER, HELSETILSTAND}