package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.arbeidsgiver.web.validation.isValidOrganisasjonsnummer
import no.nav.helse.fritakagp.domain.AgpFelter
import no.nav.helse.fritakagp.domain.ArbeidsgiverperiodeNy
import no.nav.helse.fritakagp.domain.FravaerData
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import no.nav.helse.fritakagp.domain.Periode
import no.nav.helse.fritakagp.web.api.resreq.validation.datoerHarRiktigRekkefolge
import no.nav.helse.fritakagp.web.api.resreq.validation.ikkeFlereFravaersdagerEnnDagerIMaanden
import no.nav.helse.fritakagp.web.api.resreq.validation.ingenDataEldreEnn
import no.nav.helse.fritakagp.web.api.resreq.validation.ingenDataFraFremtiden
import no.nav.helse.fritakagp.web.api.resreq.validation.isAvStorrelse
import no.nav.helse.fritakagp.web.api.resreq.validation.isGodskjentFiletyper
import no.nav.helse.fritakagp.web.api.resreq.validation.isVirksomhet
import no.nav.helse.fritakagp.web.api.resreq.validation.maanedsInntektErMellomNullOgTiMil
import no.nav.helse.fritakagp.web.api.resreq.validation.måHaAktivtArbeidsforhold
import no.nav.helse.fritakagp.web.api.resreq.validation.periodeLengdeIkkeOver16dager
import no.nav.helse.fritakagp.web.api.resreq.validation.refusjonsDagerIkkeOverstigerPeriodelengde
import org.valiktor.functions.isBetween
import org.valiktor.functions.isEmpty
import org.valiktor.functions.isEqualTo
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.isLessThanOrEqualTo
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isTrue
import org.valiktor.functions.validate
import org.valiktor.functions.validateForEach
import org.valiktor.validate

data class KroniskSoknadRequest(
    val virksomhetsnummer: String,
    val identitetsnummer: String,
    val ikkeHistoriskFravaer: Boolean,
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

            if (this@KroniskSoknadRequest.ikkeHistoriskFravaer) {
                validate(KroniskSoknadRequest::fravaer).isEmpty()
                validate(KroniskSoknadRequest::antallPerioder).isEqualTo(0)
            } else {
                validate(KroniskSoknadRequest::fravaer).isNotNull()
                validate(KroniskSoknadRequest::antallPerioder).isBetween(1, 300)
                validate(KroniskSoknadRequest::fravaer).ingenDataEldreEnn(2)
                validate(KroniskSoknadRequest::fravaer).ingenDataFraFremtiden()
                validate(KroniskSoknadRequest::fravaer).ikkeFlereFravaersdagerEnnDagerIMaanden()
            }

            if (!this@KroniskSoknadRequest.dokumentasjon.isNullOrEmpty()) {
                validate(KroniskSoknadRequest::dokumentasjon).isGodskjentFiletyper()
                validate(KroniskSoknadRequest::dokumentasjon).isAvStorrelse(SMALLEST_PDF_SIZE, 10L * MB)
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
        ikkeHistoriskFravaer = ikkeHistoriskFravaer,
        bekreftet = bekreftet,
        harVedlegg = !dokumentasjon.isNullOrEmpty()
    )
}

data class KroniskKravRequest(
    val virksomhetsnummer: String,
    val identitetsnummer: String,
    val perioder: List<ArbeidsgiverperiodeNy>,
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
                validate(ArbeidsgiverperiodeNy::perioder).periodeLengdeIkkeOver16dager()
                validate(ArbeidsgiverperiodeNy::perioder).validateForEach {
                    validate(Periode::fom).datoerHarRiktigRekkefolge(it.tom)
                    validate(Periode::fom).måHaAktivtArbeidsforhold(it, aktuelleArbeidsforhold)
                }
                validate(ArbeidsgiverperiodeNy::antallDagerMedRefusjon).refusjonsDagerIkkeOverstigerPeriodelengde(it.perioder!!)
                validate(ArbeidsgiverperiodeNy::gradering).isLessThanOrEqualTo(1.0)
                validate(ArbeidsgiverperiodeNy::gradering).isGreaterThanOrEqualTo(0.2)
                validate(ArbeidsgiverperiodeNy::månedsinntekt).maanedsInntektErMellomNullOgTiMil()
            }

            if (!this@KroniskKravRequest.dokumentasjon.isNullOrEmpty()) {
                validate(KroniskKravRequest::dokumentasjon).isGodskjentFiletyper()
                validate(KroniskKravRequest::dokumentasjon).isAvStorrelse(SMALLEST_PDF_SIZE, 10L * MB)
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
