package no.nav.helse.fritakagp.web.api.resreq

import io.ktor.application.*
import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.arbeidsgiver.web.validation.isValidOrganisasjonsnummer
import no.nav.helse.fritakagp.domain.*
import no.nav.helse.fritakagp.web.dto.validation.*
import org.valiktor.functions.hasSize
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isTrue
import org.valiktor.validate

data class KroniskSoknadRequest(
    val virksomhetsnummer: String,
    val identitetsnummer: String,
    val arbeidstyper: Set<ArbeidsType>,
    val paakjenningstyper: Set<PaakjenningsType>,
    val paakjenningBeskrivelse: String? = null,
    val fravaer: Set<FravaerData>,
    val bekreftet: Boolean,

    val dokumentasjon : String?
) {

    fun validate() {
        validate(this) {
            validate(KroniskSoknadRequest::identitetsnummer).isValidIdentitetsnummer()
            validate(KroniskSoknadRequest::bekreftet).isTrue()
            validate(KroniskSoknadRequest::virksomhetsnummer).isValidOrganisasjonsnummer()

            validate(KroniskSoknadRequest::arbeidstyper).isNotNull()
            validate(KroniskSoknadRequest::arbeidstyper).hasSize(1, 10)

            validate(KroniskSoknadRequest::paakjenningstyper).isNotNull()
            validate(KroniskSoknadRequest::paakjenningstyper).hasSize(1, 10)

            validate(KroniskSoknadRequest::fravaer).isNotNull()

            validate(KroniskSoknadRequest::fravaer).ingenDataEldreEnn(2)
            validate(KroniskSoknadRequest::fravaer).ingenDataFraFremtiden()
            validate(KroniskSoknadRequest::fravaer).ikkeFlereFravaersdagerEnnDagerIMaanden()

            if (this@KroniskSoknadRequest.paakjenningstyper.contains(PaakjenningsType.ANNET)) {
                validate(KroniskSoknadRequest::paakjenningBeskrivelse).isNotNull()
                validate(KroniskSoknadRequest::paakjenningBeskrivelse).isNotEmpty()
            }

            if (!this@KroniskSoknadRequest.dokumentasjon.isNullOrEmpty()){
                validate(KroniskSoknadRequest::dokumentasjon).isGodskjentFiletyper()
                validate(KroniskSoknadRequest::dokumentasjon).isNotStorreEnn(10L * MB)
            }
        }
    }
    
    fun toDomain(sendtAv: String) = KroniskSoeknad(
        virksomhetsnummer = virksomhetsnummer,
        identitetsnummer = identitetsnummer,
        sendtAv = sendtAv,
        arbeidstyper = arbeidstyper,
        paakjenningstyper = paakjenningstyper,
        paakjenningBeskrivelse = erstattProsentTegnMedProsent(paakjenningBeskrivelse),
        fravaer = fravaer,
        bekreftet = bekreftet,
        harVedlegg = !dokumentasjon.isNullOrEmpty()
    )
}


data class KroniskKravRequest(
        val virksomhetsnummer: String,
        val identitetsnummer: String,
        val perioder: Set<Arbeidsgiverperiode>,
        val bekreftet: Boolean,
        val dokumentasjon: String?,
        val kontrollDager: Int?,
        val antallDager: Int
) {
   fun validate() {
        validate(this) {
            validate(KroniskKravRequest::identitetsnummer).isValidIdentitetsnummer()
            validate(KroniskKravRequest::virksomhetsnummer).isValidOrganisasjonsnummer()
            validate(KroniskKravRequest::bekreftet).isTrue()
            validate(KroniskKravRequest::perioder).datoerHarRiktigRekkefolge()
            validate(KroniskKravRequest::perioder).refujonsDagerIkkeOverstigerPeriodelengder()
            validate(KroniskKravRequest::perioder).maanedsInntektErMellomNullOgTiMil()

            if (!this@KroniskKravRequest.dokumentasjon.isNullOrEmpty()) {
                validate(KroniskKravRequest::dokumentasjon).isGodskjentFiletyper()
                validate(KroniskKravRequest::dokumentasjon).isNotStorreEnn(10L * MB)
            }
        }
    }
    
    fun toDomain(sendtAv: String) = KroniskKrav(
        identitetsnummer = identitetsnummer,
        virksomhetsnummer = virksomhetsnummer,
        perioder = perioder,
        sendtAv = sendtAv,
        harVedlegg = !dokumentasjon.isNullOrEmpty(),
        kontrollDager = kontrollDager,
        antallDager = antallDager
    )
}
