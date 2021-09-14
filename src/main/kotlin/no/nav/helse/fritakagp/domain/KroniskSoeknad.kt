package no.nav.helse.fritakagp.domain

import no.nav.helse.fritakagp.db.SimpleJsonbEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class KroniskSoeknad(
    override val id: UUID = UUID.randomUUID(),
    val opprettet: LocalDateTime = LocalDateTime.now(),

    val virksomhetsnummer: String,
    val identitetsnummer: String,
    val arbeidstyper: Set<ArbeidsType>,
    val paakjenningstyper: Set<PaakjenningsType>,
    val paakjenningBeskrivelse: String? = null,
    val fravaer: Set<FravaerData>,
    val antallPerioder: Int,
    val bekreftet: Boolean,
    val harVedlegg: Boolean = false,

    var sendtAv: String,
    var virksomhetsnavn: String? = null,

    /**
     * ID fra joark etter arkivering
     */
    var journalpostId: String? = null,

    /**
     * ID fra oppgave etter opprettelse av oppgave
     */
    var oppgaveId: String? = null,
    // Må være null for tidligere verdier er lagret med null
    val sendtAvNavn: String? = null
): SimpleJsonbEntity

data class FravaerData (
    val yearMonth: String,
    val antallDagerMedFravaer: Int
) {
    fun toLocalDate() = LocalDate.parse("$yearMonth-01")
}

enum class ArbeidsType(val beskrivelse : String) {
    STILLESITTENDE("Stillesittende arbeid"),
    MODERAT("Moderat fysisk arbeid"),
    KREVENDE("Fysisk krevende arbeid")
}

enum class PaakjenningsType(val beskrivelse : String) {
    ALLERGENER("Allergener eller giftstoffer"),
    UKOMFORTABEL("Ukomfortabel temperatur eller luftfuktighet"),
    STRESSENDE("Stressende omgivelser"),
    REGELMESSIG("Regelmessige kveldsskift eller nattskift"),
    GAAING("Mye gåing/ståing"),
    HARDE("Harde gulv"),
    TUNGE("Tunge løft"),
    ANNET("Annet")
}
