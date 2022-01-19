package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.arbeidsgiver.web.validation.isValidOrganisasjonsnummer
import no.nav.helse.fritakagp.domain.*
import no.nav.helse.fritakagp.web.api.resreq.validation.*
import org.valiktor.functions.*
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
    fun validate(isVirksomhet: Boolean) {
        validate(this) {
            validate(GravidSoknadRequest::identitetsnummer).isValidIdentitetsnummer()
            validate(GravidSoknadRequest::bekreftet).isTrue()
            validate(GravidSoknadRequest::virksomhetsnummer).isValidOrganisasjonsnummer()
            validate(GravidSoknadRequest::virksomhetsnummer).isVirksomhet(isVirksomhet)

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

    fun toDomain(sendtAv: String, sendtAvNavn: String, navn: String) = GravidSoeknad(
        virksomhetsnummer = virksomhetsnummer,
        identitetsnummer = identitetsnummer,
        navn = navn,
        sendtAv = sendtAv,
        sendtAvNavn = sendtAvNavn,
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
    val perioder: List<Arbeidsgiverperiode>,
    val bekreftet: Boolean,
    val kontrollDager: Int?,
    val antallDager: Int,
    val dokumentasjon: String?
) {
    fun validate(aktuelleArbeidsforhold: List<Arbeidsforhold>) {
        validate(this) {
            validate(GravidKravRequest::antallDager).isGreaterThan(0)
            validate(GravidKravRequest::identitetsnummer).isValidIdentitetsnummer()
            validate(GravidKravRequest::virksomhetsnummer).isValidOrganisasjonsnummer()
            validate(GravidKravRequest::bekreftet).isTrue()

            validate(GravidKravRequest::perioder).validateForEach {
                validate(Arbeidsgiverperiode::fom).datoerHarRiktigRekkefolge(it.tom)
                validate(Arbeidsgiverperiode::antallDagerMedRefusjon).refusjonsDagerIkkeOverstigerPeriodelengde(it)
                validate(Arbeidsgiverperiode::månedsinntekt).maanedsInntektErMellomNullOgTiMil()
                validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforhold(it, aktuelleArbeidsforhold)
                validate(Arbeidsgiverperiode::gradering).isLessThanOrEqualTo(1.0)
                validate(Arbeidsgiverperiode::gradering).isGreaterThanOrEqualTo(0.2)
            }

            if (!this@GravidKravRequest.dokumentasjon.isNullOrEmpty()) {
                validate(GravidKravRequest::dokumentasjon).isGodskjentFiletyper()
                validate(GravidKravRequest::dokumentasjon).isNotStorreEnn(10L * MB)
            }
        }
    }

    fun toDomain(sendtAv: String, sendtAvNavn: String, navn: String) = GravidKrav(
        identitetsnummer = identitetsnummer,
        navn = navn,
        virksomhetsnummer = virksomhetsnummer,
        perioder = perioder,
        sendtAv = sendtAv,
        sendtAvNavn = sendtAvNavn,
        harVedlegg = !dokumentasjon.isNullOrEmpty(),
        kontrollDager = kontrollDager,
        antallDager = antallDager
    )
}

const val MB = 1024 * 1024
