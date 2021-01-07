package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.fritakagp.domain.GodskjentFiletyper
import no.nav.helse.arbeidsgiver.web.validation.isValidOrganisasjonsnummer
import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.web.dto.validation.isNotStorreEnn
import no.nav.helse.fritakagp.domain.OmplasseringAarsak
import no.nav.helse.fritakagp.domain.Tiltak
import org.valiktor.functions.isInIgnoringCase
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

        val datafil : String?,
        val ext : String?
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

            if (!this@GravidSoknadRequest.datafil.isNullOrEmpty()){
                validate(GravidSoknadRequest::ext).isInIgnoringCase(GodskjentFiletyper.values().map { it -> it.name })
                validate(GravidSoknadRequest::datafil).isNotStorreEnn(10L * MB)
            }
        }
    }
}

const val MB = 1024 * 1024