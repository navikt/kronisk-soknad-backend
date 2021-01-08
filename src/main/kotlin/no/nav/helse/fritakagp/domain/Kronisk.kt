package no.nav.helse.fritakagp.domain

import java.time.LocalDateTime
import java.util.*

data class SoeknadKronisk(
        val id: UUID = UUID.randomUUID(),
        val opprettet: LocalDateTime = LocalDateTime.now(),

        val orgnr: String,
        val fnr: String,
        val arbeid: List<ArbeidsType>,
        val paakjenninger: List<PaakjenningsType>,
        val paakjenningBeskrivelse: String? = null,
        val fravaer: List<FravaerData>,
        val bekreftet: Boolean,

        val sendtAv: String,

        /**
         * ID fra joark etter arkivering
         */
        var journalpostId: String? = null,

        /**
         * ID fra oppgave etter opprettelse av oppgave
         */
        var oppgaveId: String? = null
)

data class FravaerData (
    val yearMonth: String,
    val dager: String
)

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
