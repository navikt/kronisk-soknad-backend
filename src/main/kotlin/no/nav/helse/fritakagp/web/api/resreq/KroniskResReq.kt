package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.arbeidsgiver.web.validation.isValidOrganisasjonsnummer
import no.nav.helse.fritakagp.domain.*
import no.nav.helse.fritakagp.web.api.resreq.validation.*
import org.valiktor.functions.*
import org.valiktor.validate

data class KroniskSoknadRequest(
    val virksomhetsnummer: String,
    val identitetsnummer: String,
    val fravaer: Set<FravaerData>,
    val bekreftet: Boolean,
    val antallPerioder: Int,

    val dokumentasjon: String?
) {

    fun validate(isVirksomhet: Boolean) {
        validate(this) {
            validate(KroniskSoknadRequest::identitetsnummer).isValidIdentitetsnummer()
            validate(KroniskSoknadRequest::bekreftet).isTrue()
            validate(KroniskSoknadRequest::virksomhetsnummer).isValidOrganisasjonsnummer()
            validate(KroniskSoknadRequest::virksomhetsnummer).isVirksomhet(isVirksomhet)

            validate(KroniskSoknadRequest::fravaer).isNotNull()
            validate(KroniskSoknadRequest::antallPerioder).isBetween(1, 300)
            validate(KroniskSoknadRequest::fravaer).ingenDataEldreEnn(2)
            validate(KroniskSoknadRequest::fravaer).ingenDataFraFremtiden()
            validate(KroniskSoknadRequest::fravaer).ikkeFlereFravaersdagerEnnDagerIMaanden()

            if (!this@KroniskSoknadRequest.dokumentasjon.isNullOrEmpty()) {
                validate(KroniskSoknadRequest::dokumentasjon).isGodskjentFiletyper()
                validate(KroniskSoknadRequest::dokumentasjon).isNotStorreEnn(10L * MB)
            }
        }
    }

    fun toDomain(sendtAv: String, sendtAvNavn: String, navn: String) = KroniskSoeknad(
        virksomhetsnummer = virksomhetsnummer,
        identitetsnummer = identitetsnummer,
        navn = navn,
        sendtAv = sendtAv,
        sendtAvNavn = sendtAvNavn,
        antallPerioder = antallPerioder,
        fravaer = fravaer,
        bekreftet = bekreftet,
        harVedlegg = !dokumentasjon.isNullOrEmpty()
    )
}

data class KroniskKravRequest(
    val virksomhetsnummer: String,
    val identitetsnummer: String,
    val perioder: List<Arbeidsgiverperiode>,
    val bekreftet: Boolean,
    val dokumentasjon: String?,
    val kontrollDager: Int?,
    val antallDager: Int
) {
    fun validate(aktuelleArbeidsforhold: List<Arbeidsforhold>) {
        validate(this) {
            validate(KroniskKravRequest::antallDager).isGreaterThan(0)
            validate(KroniskKravRequest::antallDager).isLessThanOrEqualTo(366)
            validate(KroniskKravRequest::identitetsnummer).isValidIdentitetsnummer()
            validate(KroniskKravRequest::virksomhetsnummer).isValidOrganisasjonsnummer()
            validate(KroniskKravRequest::bekreftet).isTrue()
            validate(KroniskKravRequest::perioder).validateForEach {
                validate(Arbeidsgiverperiode::fom).datoerHarRiktigRekkefolge(it.tom)
                validate(Arbeidsgiverperiode::antallDagerMedRefusjon).refusjonsDagerIkkeOverstigerPeriodelengde(it)
                validate(Arbeidsgiverperiode::månedsinntekt).maanedsInntektErMellomNullOgTiMil()
                validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforhold(it, aktuelleArbeidsforhold)
                validate(Arbeidsgiverperiode::gradering).isLessThanOrEqualTo(1.0)
                validate(Arbeidsgiverperiode::gradering).isGreaterThanOrEqualTo(0.2)
            }

            if (!this@KroniskKravRequest.dokumentasjon.isNullOrEmpty()) {
                validate(KroniskKravRequest::dokumentasjon).isGodskjentFiletyper()
                validate(KroniskKravRequest::dokumentasjon).isNotStorreEnn(10L * MB)
            }
        }
    }

    fun toDomain(sendtAv: String, sendtAvNavn: String, navn: String) = KroniskKrav(
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
