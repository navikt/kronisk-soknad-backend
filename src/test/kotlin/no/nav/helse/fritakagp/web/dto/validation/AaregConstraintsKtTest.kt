import no.nav.helse.GravidTestData
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.*
import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import no.nav.helse.fritakagp.web.api.resreq.GravidKravRequest
import no.nav.helse.fritakagp.web.dto.validation.måHaAktivtArbeidsforhold
import org.junit.jupiter.api.Test
import org.valiktor.functions.validateForEach
import java.time.LocalDate
import java.time.LocalDateTime
import org.valiktor.validate
//import no.nav.helse.fritakagp.web.dto.validation.*

class AaregConstraintsKtTest{
    @Test
    fun `HEI HER ER EN TEST`() {

        val arbeidsgiver = Arbeidsgiver("AS", "1232242423")
        val opplysningspliktig = Opplysningspliktig("AS", "1212121212")
        val arbeidsForhold1 = Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2019, 1, 1),
                    LocalDate.of(2021, 2, 28)
                )
            ),
            LocalDateTime.now()
        )

        val arbeidsForhold2 = Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2021, 3, 1),
                    null
                )
            ),
            LocalDateTime.now()
        )


        val gravidKravRequest = GravidTestData.gravidKravRequestInValid.copy(
            perioder = listOf(
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 1, 15),
                    LocalDate.of(2021, 1, 18),
                    2,
                    månedsinntekt = 2590.8,
                ),
                Arbeidsgiverperiode(
                    LocalDate.of(2021, 2, 26),
                    LocalDate.of(2021, 3, 10),
                    12,
                    månedsinntekt = 2590.8,
                )
            )
        )
        validate(gravidKravRequest){

            validate(GravidKravRequest::perioder).validateForEach {
                validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforhold(it, listOf(arbeidsForhold1,arbeidsForhold2))
            }
        }


    }

}