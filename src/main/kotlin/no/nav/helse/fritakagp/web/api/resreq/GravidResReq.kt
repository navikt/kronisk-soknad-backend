package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.arbeidsgiver.web.validation.isValidOrganisasjonsnummer
import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.domain.OmplasseringAarsak
import no.nav.helse.fritakagp.domain.Tiltak
import no.nav.helse.fritakagp.web.dto.validation.isGodskjentFiletyper
import no.nav.helse.fritakagp.web.dto.validation.isNotStorreEnn
import no.nav.helse.fritakagp.web.dto.validation.refusjonsDagerIkkeOverstigerPeriodelengde
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isTrue
import org.valiktor.validate

data class GravidSoknadRequest(
    val orgnr: String,
    val fnr: String,
    val tilrettelegge: Boolean,

    val tiltak: List<Tiltak>? = null,
    val tiltakBeskrivelse: String? = null,

    val omplassering: Omplassering? = null,
    val omplasseringAarsak: OmplasseringAarsak? = null,
    val bekreftet: Boolean,

    val dokumentasjon: String?
) {
    init {
        validate(this) {
            validate(GravidSoknadRequest::fnr).isValidIdentitetsnummer()
            validate(GravidSoknadRequest::bekreftet).isTrue()
            validate(GravidSoknadRequest::orgnr).isValidOrganisasjonsnummer()


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
}


data class GravidKravRequest(
    val virksomhetsnummer: String,
    val identitetsnummer: String,
    val periode: Arbeidsgiverperiode,

    val bekreftet: Boolean,

    val dokumentasjon: String?
) {
    init {
        validate(this) {
            validate(GravidKravRequest::identitetsnummer).isValidIdentitetsnummer()
            validate(GravidKravRequest::virksomhetsnummer).isValidOrganisasjonsnummer()
            validate(GravidKravRequest::bekreftet).isTrue()
            validate(GravidKravRequest::periode).refusjonsDagerIkkeOverstigerPeriodelengde()

            if (!this@GravidKravRequest.dokumentasjon.isNullOrEmpty()) {
                validate(GravidKravRequest::dokumentasjon).isGodskjentFiletyper()
                validate(GravidKravRequest::dokumentasjon).isNotStorreEnn(10L * MB)
            }
        }
    }
}


const val MB = 1024 * 1024
