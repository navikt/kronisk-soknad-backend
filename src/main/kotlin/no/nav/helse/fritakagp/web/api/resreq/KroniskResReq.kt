package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.arbeidsgiver.web.validation.isValidOrganisasjonsnummer
import no.nav.helse.fritakagp.domain.ArbeidsType
import no.nav.helse.fritakagp.domain.FravaerData
import no.nav.helse.fritakagp.domain.PaakjenningsType
import no.nav.helse.fritakagp.web.dto.validation.*
import org.valiktor.functions.hasSize
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isTrue
import org.valiktor.validate

data class KroniskSoknadRequest(
    val orgnr: String,
    val fnr: String,
    val arbeidstyper: Set<ArbeidsType>,
    val paakjenningstyper: Set<PaakjenningsType>,
    val paakjenningBeskrivelse: String? = null,
    val fravaer: Set<FravaerData>,
    val bekreftet: Boolean,

    val dokumentasjon : String?
) {
    init {
        validate(this) {
            validate(KroniskSoknadRequest::fnr).isValidIdentitetsnummer()
            validate(KroniskSoknadRequest::bekreftet).isTrue()
            validate(KroniskSoknadRequest::orgnr).isValidOrganisasjonsnummer()

            validate(KroniskSoknadRequest::arbeidstyper).isNotNull()
            validate(KroniskSoknadRequest::arbeidstyper).hasSize(1, 10)

            validate(KroniskSoknadRequest::paakjenningstyper).isNotNull()
            validate(KroniskSoknadRequest::paakjenningstyper).hasSize(1, 10)

            validate(KroniskSoknadRequest::fravaer).isNotNull()

            validate(KroniskSoknadRequest::fravaer).ingenDataEldreEnn(3)
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
}
