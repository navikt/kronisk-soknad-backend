package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.arbeidsgiver.web.validation.isValidOrganisasjonsnummer
import no.nav.helse.fritakagp.domain.ArbeidsType
import no.nav.helse.fritakagp.domain.GodskjentFiletyper
import no.nav.helse.fritakagp.domain.PaakjenningsType
import no.nav.helse.fritakagp.web.dto.validation.isNotStorreEnn
import org.valiktor.functions.isInIgnoringCase
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isTrue
import org.valiktor.validate

data class KroniskSoknadRequest(
        val orgnr: String,
        val fnr: String,
        val arbeid: List<ArbeidsType>,
        val paakjenninger: List<PaakjenningsType>,
        val paakjenningBeskrivelse: String? = null,
        val fravaer: Any, // ToDo: typesafety
        val bekreftet: Boolean,

        val datafil : String?,
        val ext : String?
) {
    init {
        validate(this) {
            validate(KroniskSoknadRequest::fnr).isValidIdentitetsnummer()
            validate(KroniskSoknadRequest::bekreftet).isTrue()
            validate(KroniskSoknadRequest::orgnr).isValidOrganisasjonsnummer()
            validate(KroniskSoknadRequest::arbeid).isNotNull()
            validate(KroniskSoknadRequest::paakjenninger).isNotNull()
            validate(KroniskSoknadRequest::fravaer).isNotNull()

            if (this@KroniskSoknadRequest.paakjenninger.contains(PaakjenningsType.ANNET) == true) {
                validate(KroniskSoknadRequest::paakjenningBeskrivelse).isNotNull()
                validate(KroniskSoknadRequest::paakjenningBeskrivelse).isNotEmpty()
            }

            if (!this@KroniskSoknadRequest.datafil.isNullOrEmpty()){
                validate(KroniskSoknadRequest::ext).isInIgnoringCase(GodskjentFiletyper.values().map { it -> it.name })
                validate(KroniskSoknadRequest::datafil).isNotStorreEnn(10L * MB)
            }
        }
    }
}
