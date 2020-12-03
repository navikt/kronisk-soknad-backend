package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.fritakagp.domain.GodskjentFiletyper
import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.web.dto.validation.isNotStorreEnn
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
        val omplasseringAarsak: String?,
        val datafil : String?,
        val ext : String?
) {
    init {
        validate(this) {
            validate(GravideSoknadRequest::fnr).isValidIdentitetsnummer()
            validate(GravideSoknadRequest::tilrettelegge).isNotNull()
            validate(GravideSoknadRequest::omplassering).isInIgnoringCase(Omplassering.values().map { it -> it.name })
            validate(GravideSoknadRequest::omplasseringAarsak).isOmplasseringValgRiktig(it.omplassering)
            validate(GravideSoknadRequest::tiltak).isTiltakValid(it.tiltakBeskrivelse)
            datafil?.let {
                validate(GravideSoknadRequest::ext).isInIgnoringCase(GodskjentFiletyper.values().map { it -> it.name })
                validate(GravideSoknadRequest::datafil).isNotStorreEnn(10L * MB)
            }
        }
    }
}
const val GB = 1024 * 1024 * 1024
const val MB = 1024 * 1024