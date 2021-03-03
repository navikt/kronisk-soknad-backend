package no.nav.helse.fritakagp.web.api.resreq

import io.ktor.application.*
import no.nav.helse.arbeidsgiver.web.validation.isValidIdentitetsnummer
import no.nav.helse.arbeidsgiver.web.validation.isValidOrganisasjonsnummer
import no.nav.helse.fritakagp.domain.*
import no.nav.helse.fritakagp.web.dto.validation.isGodskjentFiletyper
import no.nav.helse.fritakagp.web.dto.validation.isNotStorreEnn
import no.nav.helse.fritakagp.web.dto.validation.refusjonsDagerIkkeOverstigerPeriodelengde
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isTrue
import org.valiktor.validate
import java.time.LocalDate

data class GravidSoknadRequest(
        val virksomhetsnummer: String,
        val identitetsnummer: String,
        val tilrettelegge: Boolean,
        val termindato: LocalDate?,

        val tiltak: List<Tiltak>? = null,
        val tiltakBeskrivelse: String? = null,

        val omplassering: Omplassering? = null,
        val omplasseringAarsak: OmplasseringAarsak? = null,
        val bekreftet: Boolean,



        val dokumentasjon: String?
) {
   fun validate() {
       validate(this) {
           validate(GravidSoknadRequest::identitetsnummer).isValidIdentitetsnummer()
           validate(GravidSoknadRequest::bekreftet).isTrue()
           validate(GravidSoknadRequest::virksomhetsnummer).isValidOrganisasjonsnummer()


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
    
    fun toDomain(sendtAv: String) = GravidSoeknad(
            virksomhetsnummer = virksomhetsnummer,
            identitetsnummer = identitetsnummer,
            sendtAv = sendtAv,
            termindato = termindato,
            omplassering = omplassering,
            omplasseringAarsak = omplasseringAarsak,
            tilrettelegge = tilrettelegge,
            tiltak = tiltak,
            tiltakBeskrivelse = tiltakBeskrivelse,
            harVedlegg = !dokumentasjon.isNullOrEmpty()
        )
}


data class GravidKravRequest(
    val virksomhetsnummer: String,
    val identitetsnummer: String,
    val periode: Arbeidsgiverperiode,

    val bekreftet: Boolean,
    val kontrollDager: Int?,
    val dokumentasjon: String?
) {
    fun validate() {
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
    
    fun toDomain(sendtAv: String) = GravidKrav(
        identitetsnummer = identitetsnummer,
        virksomhetsnummer = virksomhetsnummer,
        periode = periode,
        sendtAv = sendtAv,
        harVedlegg = !dokumentasjon.isNullOrEmpty(),
        kontrollDager = kontrollDager
    )
    
}


const val MB = 1024 * 1024
