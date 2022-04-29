package no.nav.helse.fritakagp.web.dto.validation

import no.nav.helse.AaregTestData
import no.nav.helse.GravidTestData
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.*
import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import no.nav.helse.fritakagp.web.api.resreq.GravidKravRequest
import no.nav.helse.fritakagp.web.api.resreq.validation.måHaAktivtArbeidsforhold
import no.nav.helse.fritakagp.web.api.resreq.validation.slåSammenPerioder
import no.nav.helse.fritakagp.web.api.resreq.validationShouldFailFor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.valiktor.functions.validateForEach
import java.time.LocalDate
import java.time.LocalDateTime
import org.valiktor.validate
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.helse.arbeidsgiver.utils.loadFromResources

class AaregConstraintsKtTest {
    @Test
    @Disabled
    fun `Rådata fra aareg (Brukes for å feilsøke med respons fra AA-reg)`() {
        val om = ObjectMapper()
        om.registerModule(KotlinModule())
        om.registerModule(Jdk8Module())
        om.registerModule(JavaTimeModule())
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        om.configure(SerializationFeature.INDENT_OUTPUT, true)
        om.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        om.setDefaultPrettyPrinter(
            DefaultPrettyPrinter().apply {
                indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                indentObjectsWith(DefaultIndenter("  ", "\n"))
            }
        )

        // Legg aareg JSON-respons i src/test/resources/aareg.json
        val aaregFile = "aareg.json".loadFromResources()
        val arbeidsforhold = om.readValue<List<Arbeidsforhold>>(aaregFile)
            // Legg inn organisasjonsnummer
            .filter { it.arbeidsgiver.organisasjonsnummer == "XXXXXXXX" }

        // Endre til perioden kravet gjelder
        val arbeidsgiverPeriode = Arbeidsgiverperiode(
            LocalDate.of(2021, 1, 15),
            LocalDate.of(2021, 1, 20),
            4,
            månedsinntekt = 2590.8,
        )

        validate(arbeidsgiverPeriode) {
            validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforhold(arbeidsgiverPeriode, arbeidsforhold)
        }
    }

    @Test
    fun `Ansatt slutter fram i tid`() {
        val periode = Arbeidsgiverperiode(
            LocalDate.of(2021, 1, 15),
            LocalDate.of(2021, 1, 20),
            4,
            månedsinntekt = 2590.8,
        )

        validate(periode) {
            validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforhold(periode, AaregTestData.arbeidsforholdMedSluttDato)
        }
    }

    @Test
    fun `Refusjonskravet er innenfor Arbeidsforholdet`() {
        val periode = Arbeidsgiverperiode(
            LocalDate.of(2021, 1, 15),
            LocalDate.of(2021, 1, 18),
            2,
            månedsinntekt = 2590.8,
        )

        validate(periode) {
            validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforhold(periode, AaregTestData.evigArbeidsForholdListe)
        }
    }

    @Test
    fun `Sammenehengende arbeidsforhold slås sammen til en periode`() {

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
        validate(gravidKravRequest) {
            validate(GravidKravRequest::perioder).validateForEach {
                validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforhold(
                    it,
                    listOf(arbeidsForhold1, arbeidsForhold2)
                )
            }
        }
    }

    @Test
    fun `Refusjonsdato er før Arbeidsforhold har begynt`() {

        val periode = Arbeidsgiverperiode(
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 1, 5),
            2,
            månedsinntekt = 2590.8,
        )
        validationShouldFailFor(Arbeidsgiverperiode::fom) {
            validate(periode) {
                validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforhold(
                    periode,
                    AaregTestData.pågåendeArbeidsforholdListe
                )
            }
        }
    }

    @Test
    fun `Refusjonsdato etter Arbeidsforhold er avsluttet`() {
        val periode = Arbeidsgiverperiode(
            LocalDate.of(2021, 5, 15),
            LocalDate.of(2021, 5, 18),
            2,
            månedsinntekt = 2590.8,
        )

        validationShouldFailFor(Arbeidsgiverperiode::fom) {
            validate(periode) {
                validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforhold(
                    periode,
                    AaregTestData.avsluttetArbeidsforholdListe
                )
            }
        }
    }

    @Test
    fun `merge fragmented periods`() {
        assertThat(
            slåSammenPerioder(
                listOf(
                    // skal ble merget til 1 periode fra 1.1.21 til 28.2.21
                    Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 29)),
                    Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 13)),
                    Periode(LocalDate.of(2021, 2, 15), LocalDate.of(2021, 2, 28)),

                    // skal bli merget til 1
                    Periode(LocalDate.of(2021, 3, 20), LocalDate.of(2021, 3, 31)),
                    Periode(LocalDate.of(2021, 4, 2), LocalDate.of(2021, 4, 30)),

                    // skal bli merget til 1
                    Periode(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 8, 30)),
                    Periode(LocalDate.of(2021, 9, 1), null),
                )
            )
        ).hasSize(3)

        assertThat(
            slåSammenPerioder(
                listOf(
                    Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 29)),
                    Periode(LocalDate.of(2021, 9, 1), null),
                )
            )
        ).hasSize(2)

        assertThat(
            slåSammenPerioder(
                listOf(
                    Periode(LocalDate.of(2021, 9, 1), null),
                )
            )
        ).hasSize(1)
    }
}
