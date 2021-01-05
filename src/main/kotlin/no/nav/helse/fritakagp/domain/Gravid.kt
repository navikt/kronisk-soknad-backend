package no.nav.helse.fritakagp.domain

import java.time.LocalDateTime
import java.util.*

data class SoeknadGravid(
        val id: UUID = UUID.randomUUID(),
        val opprettet: LocalDateTime = LocalDateTime.now(),

        val orgnr: String,
        val fnr: String,
        val tilrettelegge: Boolean,
        val tiltak: List<Tiltak>? = null,
        val tiltakBeskrivelse: String? = null,
        val omplassering: Omplassering?,
        val omplasseringAarsak: OmplasseringAarsak? = null,
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

fun decodeBase64File(datafile: String): ByteArray {
    return Base64.getDecoder().decode(datafile)
}

fun getTiltakValue(req : List<String>) : List<Tiltak> {
    return req.map { it -> Tiltak.valueOf(it.toUpperCase()) }
}

fun getTiltakBeskrivelse(req : List<String>) : List<String> {
    return req.map { it -> Tiltak.valueOf(it.toUpperCase()).beskrivelse }
}
fun getOmplasseringValue(req : String) : OmplasseringAarsak {
    return OmplasseringAarsak.valueOf(req.toUpperCase())
}
fun getOmplasseringBeskrivelse(req : String) : String {
    return OmplasseringAarsak.valueOf(req.toUpperCase()).beskrivelse
}

enum class Omplassering(val beskrivelse: String) {
    JA("Ja"),
    NEI("Nei"),
    IKKE_MULIG("Ikke mulig")
}
enum class Tiltak(val beskrivelse : String) {
    TILPASSET_ARBEIDSTID("Fleksibel eller tilpasset arbeidstid"),
    HJEMMEKONTOR("Hjemmekontor"),
    TILPASSEDE_ARBEIDSOPPGAVER("Tilpassede arbeidsoppgaver"),
    ANNET("Annet")
}

enum class OmplasseringAarsak(val beskrivelse : String) {
    MOTSETTER("Den ansatte motsetter seg omplassering"),
    FAAR_IKKE_KONTAKT("Vi får ikke kontakt med den ansatte"),
    IKKE_ANDRE_OPPGAVER("Vi har ikke andre oppgaver eller arbeidssteder å tilby"),
    HELSETILSTANDEN("Den ansatte vil ikke fungere i en annen jobb på grunn av helsetilstanden")
}
enum class GodskjentFiletyper(val beskrivelse : String) {
    PDF("pdf"),
    JPEG("jpg"),
    PNG("png")
}