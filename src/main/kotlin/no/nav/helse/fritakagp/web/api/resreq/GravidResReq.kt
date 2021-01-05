package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.arbeidsgiver.web.validation.isValidOrganisasjonsnummer
import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.domain.OmplasseringAarsak
import no.nav.helse.fritakagp.domain.Tiltak
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isTrue
import org.valiktor.validate

data class GravideSoknadRequest(
        val orgnr: String,
        val fnr: String,
        val tilrettelegge: Boolean,

        val tiltak: List<Tiltak>? = null,
        val tiltakBeskrivelse: String? = null,

        val omplassering: Omplassering? = null,
        val omplasseringAarsak: OmplasseringAarsak? = null,

        val bekreftet: Boolean
) {
    init {
        validate(this) {
            validate(GravideSoknadRequest::fnr).isValidIdentitetsnummer()
            validate(GravideSoknadRequest::bekreftet).isTrue()
            validate(GravideSoknadRequest::orgnr).isValidOrganisasjonsnummer()


            if (this@GravideSoknadRequest.tilrettelegge) {
                validate(GravideSoknadRequest::tiltak).isNotNull()

                if (this@GravideSoknadRequest.tiltak?.contains(Tiltak.ANNET) == true) {
                    validate(GravideSoknadRequest::tiltakBeskrivelse).isNotNull()
                    validate(GravideSoknadRequest::tiltakBeskrivelse).isNotEmpty()
                }

                if (this@GravideSoknadRequest.omplassering == Omplassering.IKKE_MULIG) {
                    validate(GravideSoknadRequest::omplasseringAarsak).isNotNull()
                }
            }
        }
    }
}