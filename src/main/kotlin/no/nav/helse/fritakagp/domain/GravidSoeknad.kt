package no.nav.helse.fritakagp.domain

import no.nav.helse.fritakagp.db.SimpleJsonbEntity
import java.time.LocalDateTime
import java.util.*

data class GravidSoeknad(
        override val id: UUID = UUID.randomUUID(),
        val opprettet: LocalDateTime = LocalDateTime.now(),

        val virksomhetsnummer: String,
        val identitetsnummer: String,
        val tilrettelegge: Boolean,
        val tiltak: List<Tiltak>? = null,
        val tiltakBeskrivelse: String? = null,
        val omplassering: Omplassering?,
        val omplasseringAarsak: OmplasseringAarsak? = null,
        val sendtAv: String,
        val termindato: LocalDateTime?,

        /**
     * ID fra joark etter arkivering
     */
        var journalpostId: String? = null,

        /**
     * ID fra oppgave etter opprettelse av oppgave
     */
        var oppgaveId: String? = null
) : SimpleJsonbEntity

enum class Omplassering(val beskrivelse: String) {
    JA("Ja"),
    NEI("Nei"),
    IKKE_MULIG("Ikke mulig")
}

enum class Tiltak(val beskrivelse: String) {
    TILPASSET_ARBEIDSTID("Fleksibel eller tilpasset arbeidstid"),
    HJEMMEKONTOR("Hjemmekontor"),
    TILPASSEDE_ARBEIDSOPPGAVER("Tilpassede arbeidsoppgaver"),
    ANNET("Annet")
}

enum class OmplasseringAarsak(val beskrivelse: String) {
    MOTSETTER("Den ansatte motsetter seg omplassering"),
    FAAR_IKKE_KONTAKT("Vi får ikke kontakt med den ansatte"),
    IKKE_ANDRE_OPPGAVER("Vi har ikke andre oppgaver eller arbeidssteder å tilby"),
    HELSETILSTANDEN("Den ansatte vil ikke fungere i en annen jobb på grunn av helsetilstanden")
}
