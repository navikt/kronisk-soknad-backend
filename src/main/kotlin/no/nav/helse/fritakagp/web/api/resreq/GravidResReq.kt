package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.fritakagp.web.dto.validation.isOmplasseringValgRiktig
import no.nav.helse.fritakagp.web.dto.validation.isTiltakValid
import org.valiktor.functions.isInIgnoringCase
import org.valiktor.functions.isNotNull
import org.valiktor.validate
import java.time.LocalDate

data class GravideSoknadRequest(
        val dato: LocalDate,
        val fnr: String,
        val tilrettelegge: Boolean,
        val tiltak: List<String>,
        val tiltakBeskrivelse: String?,
        val omplassering: String,
        val omplasseringAarsak: String?
) {
    init {
        validate(this) {
            validate(GravideSoknadRequest::fnr).isValidIdentitetsnummer()
            validate(GravideSoknadRequest::tiltak).isTiltakValid(it.tiltakBeskrivelse)
            validate(GravideSoknadRequest::tilrettelegge).isNotNull()
            validate(GravideSoknadRequest::omplassering).isInIgnoringCase("Ja","Nei", "Ikke_mulig")
            validate(GravideSoknadRequest::omplasseringAarsak).isOmplasseringValgRiktig(it.omplassering)

        }

    }
}