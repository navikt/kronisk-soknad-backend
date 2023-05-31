package no.nav.helse.fritakagp.domain

import no.nav.helse.fritakagp.db.SimpleJsonbEntity
import java.time.LocalDateTime
import java.util.UUID

data class GravidKrav(
    override val id: UUID = UUID.randomUUID(),
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val sendtAv: String,

    val virksomhetsnummer: String,
    val identitetsnummer: String,
    // Må være null for tidligere verdier er lagret med null
    var navn: String? = null,
    val perioder: List<ArbeidsgiverperiodeNy>,
    val harVedlegg: Boolean = false,
    val kontrollDager: Int?,
    val antallDager: Int,

    var journalpostId: String? = null,

    var oppgaveId: String? = null,
    var virksomhetsnavn: String? = null,
    // Må være null for tidligere verdier er lagret med null
    var sendtAvNavn: String? = null,

    var sletteJournalpostId: String? = null,
    var sletteOppgaveId: String? = null,
    var slettetAv: String? = null,
    var slettetAvNavn: String? = null,

    var status: KravStatus = KravStatus.OPPRETTET,
    var aarsakEndring: String? = null,
    var endretDato: LocalDateTime? = null,

    var arbeidsgiverSakId: String? = null,
    var referansenummer: Int? = null
) : SimpleJsonbEntity {

    fun toKravForOppgave(): List<KravForOppgave> {
        return perioder.map {
            KravForOppgave(
                KravType.GRAVID,
                id = id,
                opprettet = opprettet,
                sendtAv = sendtAv,
                virksomhetsnummer = virksomhetsnummer,
                identitetsnummer = identitetsnummer,
                navn = navn,
                perioder = it.tilArbeidsgiverperideLegacy(),
                harVedlegg = harVedlegg,
                kontrollDager = kontrollDager,
                antallDager = antallDager,
                journalpostId = journalpostId,
                oppgaveId = it.oppgaveId,
                virksomhetsnavn = virksomhetsnavn,
                sendtAvNavn = sendtAvNavn,
                status = status,
                arbeidsgiverSakId = arbeidsgiverSakId,
                referansenummer = referansenummer
            )
        }
    }
    companion object {
        const val tittel = "Krav om refusjon av arbeidsgiverperioden - graviditet"
    }
}

enum class KravStatus {
    OPPRETTET,
    ENDRET,
    SLETTET
}
