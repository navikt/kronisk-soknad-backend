package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.arbeidsgiver.web.validation.isValidOrganisasjonsnummer
import no.nav.helse.fritakagp.domain.*
import no.nav.helse.fritakagp.web.dto.validation.*
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isTrue
import org.valiktor.validate
import java.time.LocalDate

data class GravidSoknadRequest(
    val virksomhetsnummer: String,
    val identitetsnummer: String,
    val tilrettelegge: Boolean,
    val termindato: LocalDate?,

    val tiltak: List<Tiltak>? = null,
    val tiltakBeskrivelse: String? = null,

    val omplassering: Omplassering? = null,
    val omplasseringAarsak: OmplasseringAarsak? = null,
    val bekreftet: Boolean,

    val dokumentasjon: String?
) {
    fun validate() {
        validate(this) {
            validate(GravidSoknadRequest::identitetsnummer).isValidIdentitetsnummer()
            validate(GravidSoknadRequest::bekreftet).isTrue()
            validate(GravidSoknadRequest::virksomhetsnummer).isValidOrganisasjonsnummer()


            if (this@GravidSoknadRequest.tilrettelegge) {
                validate(GravidSoknadRequest::tiltak).isNotNull()

                if (this@GravidSoknadRequest.tiltak?.contains(Tiltak.ANNET) == true) {
                    validate(GravidSoknadRequest::tiltakBeskrivelse).isNotNull()
                    validate(GravidSoknadRequest::tiltakBeskrivelse).isNotEmpty()
                }

                if (this@GravidSoknadRequest.omplassering == Omplassering.IKKE_MULIG) {
                    validate(GravidSoknadRequest::omplasseringAarsak).isNotNull()
                }
            }

            if (!this@GravidSoknadRequest.dokumentasjon.isNullOrEmpty()) {
                validate(GravidSoknadRequest::dokumentasjon).isGodskjentFiletyper()
                validate(GravidSoknadRequest::dokumentasjon).isNotStorreEnn(10L * MB)
            }
        }
    }

    fun toDomain(sendtAv: String) = GravidSoeknad(
        virksomhetsnummer = virksomhetsnummer,
        identitetsnummer = identitetsnummer,
        sendtAv = sendtAv,
        termindato = termindato,
        omplassering = omplassering,
        omplasseringAarsak = omplasseringAarsak,
        tilrettelegge = tilrettelegge,
        tiltak = tiltak,
        tiltakBeskrivelse = tiltakBeskrivelse,
        harVedlegg = !dokumentasjon.isNullOrEmpty()
    )
}


data class GravidKravRequest(
    val virksomhetsnummer: String,
    val identitetsnummer: String,
    val perioder: Set<Arbeidsgiverperiode>,

    val bekreftet: Boolean,
    val kontrollDager: Int?,
    val antallDager: Int,
    val dokumentasjon: String?
) {
    fun validate() {
        validate(this) {
            validate(GravidKravRequest::identitetsnummer).isValidIdentitetsnummer()
            validate(GravidKravRequest::virksomhetsnummer).isValidOrganisasjonsnummer()
            validate(GravidKravRequest::bekreftet).isTrue()
            validate(GravidKravRequest::perioder).datoerHarRiktigRekkefolge()
            validate(GravidKravRequest::perioder).refujonsDagerIkkeOverstigerPeriodelengder()
            validate(GravidKravRequest::perioder).maanedsInntektErMellomNullOgTiMil()

            if (!this@GravidKravRequest.dokumentasjon.isNullOrEmpty()) {
                validate(GravidKravRequest::dokumentasjon).isGodskjentFiletyper()
                validate(GravidKravRequest::dokumentasjon).isNotStorreEnn(10L * MB)
            }
        }
    }

    fun toDomain(sendtAv: String) = GravidKrav(
        identitetsnummer = identitetsnummer,
        virksomhetsnummer = virksomhetsnummer,
        perioder = perioder,
        sendtAv = sendtAv,
        harVedlegg = !dokumentasjon.isNullOrEmpty(),
        kontrollDager = kontrollDager,
        antallDager = antallDager
    )

}


const val MB = 1024 * 1024