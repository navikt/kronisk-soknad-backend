package no.nav.helse.fritakagp.web.dto.validation

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.AaregTestData
import no.nav.helse.GravidTestData
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Ansettelsesperiode
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsgiver
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Opplysningspliktig
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Periode as AAregPeriode
import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.helse.fritakagp.customObjectMapper
import no.nav.helse.fritakagp.domain.AgpFelter
import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import no.nav.helse.fritakagp.domain.ArbeidsgiverperiodeNy
import no.nav.helse.fritakagp.domain.Periode
import no.nav.helse.fritakagp.web.api.resreq.GravidKravRequest
import no.nav.helse.fritakagp.web.api.resreq.validation.måHaAktivtArbeidsforhold
import no.nav.helse.fritakagp.web.api.resreq.validation.slåSammenPerioder
import no.nav.helse.fritakagp.web.api.resreq.validationShouldFailFor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.valiktor.functions.validateForEach
import org.valiktor.validate
import java.time.LocalDate
import java.time.LocalDateTime

class AaregConstraintsKtTest {
    @Test
    @Disabled
    fun `Rådata fra aareg (Brukes for å feilsøke med respons fra AA-reg)`() {
        val objectMapper = customObjectMapper()

        // Legg aareg JSON-respons i src/test/resources/aareg.json
        val aaregFile = "aareg.json".loadFromResources()
        val arbeidsforhold = objectMapper.readValue<List<Arbeidsforhold>>(aaregFile)
            // Legg inn organisasjonsnummer
            .filter { it.arbeidsgiver.organisasjonsnummer == "XXXXXXXX" }

        // Endre til perioden kravet gjelder
        val arbeidsgiverPeriode = Arbeidsgiverperiode(
            LocalDate.of(2021, 1, 15),
            LocalDate.of(2021, 1, 20),
            4,
            månedsinntekt = 2590.8
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
            månedsinntekt = 2590.8
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
            månedsinntekt = 2590.8
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
                AAregPeriode(
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
                AAregPeriode(
                    LocalDate.of(2021, 3, 1),
                    null
                )
            ),
            LocalDateTime.now()
        )

        val gravidKravRequest = GravidTestData.gravidKravRequestInValid.copy(
            perioder = listOf(
                ArbeidsgiverperiodeNy(
                    perioder = listOf(
                        Periode(
                            LocalDate.of(2021, 1, 15),
                            LocalDate.of(2021, 1, 18)
                        )
                    ),
                    antallDagerMedRefusjon = 2, månedsinntekt = 2590.8
                ),
                ArbeidsgiverperiodeNy(
                    LocalDate.of(2021, 2, 26),
                    LocalDate.of(2021, 3, 10),
                    antallDagerMedRefusjon = 12, månedsinntekt = 2590.0
                )
            )
        )
        validate(gravidKravRequest) {
            validate(GravidKravRequest::perioder).validateForEach {
                validate(ArbeidsgiverperiodeNy::perioder).validateForEach {
                    validate(Periode::fom).måHaAktivtArbeidsforhold(it, listOf(arbeidsForhold1, arbeidsForhold2))
                }
            }
        }
    }

    @Test
    fun `Refusjonsdato er før Arbeidsforhold har begynt`() {
        val periode = Arbeidsgiverperiode(
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 1, 5),
            2,
            månedsinntekt = 2590.8
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
    fun `Refusjonsdato begynner samtidig som Arbeidsforhold skal ikke feile`() {
        val periode = Arbeidsgiverperiode(
            LocalDate.of(2021, 2, 5),
            LocalDate.of(2021, 2, 9),
            2,
            månedsinntekt = 2590.8
        )
        validate(periode) {
            validate(Arbeidsgiverperiode::fom).måHaAktivtArbeidsforhold(
                periode,
                AaregTestData.pågåendeArbeidsforholdListe
            )
        }
    }

    @Test
    fun `Refusjonsdato etter Arbeidsforhold er avsluttet`() {
        val periode = Arbeidsgiverperiode(
            LocalDate.of(2021, 5, 15),
            LocalDate.of(2021, 5, 18),
            2,
            månedsinntekt = 2590.8
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
                    AAregPeriode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 29)),
                    AAregPeriode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 13)),
                    AAregPeriode(LocalDate.of(2021, 2, 15), LocalDate.of(2021, 2, 28)),

                    // skal bli merget til 1
                    AAregPeriode(LocalDate.of(2021, 3, 20), LocalDate.of(2021, 3, 31)),
                    AAregPeriode(LocalDate.of(2021, 4, 2), LocalDate.of(2021, 4, 30)),

                    // skal bli merget til 1
                    AAregPeriode(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 8, 30)),
                    AAregPeriode(LocalDate.of(2021, 9, 1), null)
                )
            )
        ).hasSize(3)

        assertThat(
            slåSammenPerioder(
                listOf(
                    AAregPeriode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 29)),
                    AAregPeriode(LocalDate.of(2021, 9, 1), null)
                )
            )
        ).hasSize(2)

        assertThat(
            slåSammenPerioder(
                listOf(
                    AAregPeriode(LocalDate.of(2021, 9, 1), null)
                )
            )
        ).hasSize(1)
    }
}
