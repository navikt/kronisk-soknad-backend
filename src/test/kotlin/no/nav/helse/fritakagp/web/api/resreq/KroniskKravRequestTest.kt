package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.AaregTestData
import no.nav.helse.KroniskTestData
import no.nav.helse.fritakagp.domain.AgpFelter
import no.nav.helse.fritakagp.domain.Periode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KroniskKravRequestTest {
    val navn = "Personliga Person"
    val sendtAv = "123"
    val sendtAvNavn = "Ola M Avsender"

    @Test
    internal fun `Antall dager kan ikke være mer enn dager i året`() {
        validationShouldFailFor(KroniskKravRequest::antallDager) {
            KroniskTestData.kroniskKravRequestValid.copy(antallDager = 367).validate(AaregTestData.evigArbeidsForholdListe)
        }
    }

    @Test
    internal fun `Antall dager kan ikke være negativt`() {
        validationShouldFailFor(KroniskKravRequest::antallDager) {
            KroniskTestData.kroniskKravRequestValid.copy(antallDager = -1).validate(AaregTestData.evigArbeidsForholdListe)
        }
    }

    @Test
    internal fun `Antall dager må være 1-366`() {
        validationShouldFailFor(KroniskKravRequest::antallDager) {
            KroniskTestData.kroniskKravRequestValid.copy(antallDager = 0).validate(AaregTestData.evigArbeidsForholdListe)
        }
        validationShouldFailFor(KroniskKravRequest::antallDager) {
            KroniskTestData.kroniskKravRequestValid.copy(antallDager = 367).validate(AaregTestData.evigArbeidsForholdListe)
        }
    }

    @Test
    internal fun `Gyldig FNR er påkrevd`() {
        validationShouldFailFor(KroniskKravRequest::identitetsnummer) {
            KroniskTestData.kroniskKravRequestValid.copy(identitetsnummer = "01020312345").validate(AaregTestData.evigArbeidsForholdListe)
        }
    }

    @Test
    internal fun `Gyldig OrgNr er påkrevd dersom det er oppgitt`() {
        validationShouldFailFor(KroniskKravRequest::virksomhetsnummer) {
            KroniskTestData.kroniskKravRequestValid.copy(virksomhetsnummer = "098765432").validate(AaregTestData.evigArbeidsForholdListe)
        }
    }

    @Test
    internal fun `Bekreftelse av egenerklæring er påkrevd`() {
        validationShouldFailFor(KroniskKravRequest::bekreftet) {
            KroniskTestData.kroniskKravRequestValid.copy(bekreftet = false).validate(AaregTestData.evigArbeidsForholdListe)
        }
    }

    @Test
    internal fun `mapping til domenemodell tar med harVedleggflagg`() {
        Assertions.assertThat(KroniskTestData.kroniskKravRequestMedFil.toDomain(sendtAv, sendtAvNavn, navn).harVedlegg).isTrue
        Assertions.assertThat(KroniskTestData.kroniskKravRequestValid.toDomain(sendtAv, sendtAvNavn, navn).harVedlegg).isFalse
    }

    @Test
    internal fun `Antall refusjonsdager kan ikke overstige periodelengden`() {
        validationShouldFailFor("perioder[0].antallDagerMedRefusjon") {
            KroniskTestData.kroniskKravRequestValid.copy(
                perioder = listOf(KroniskTestData.kroniskKravRequestValid.perioder.first().copy().also { it.felter = it.felter.copy(antallDagerMedRefusjon = 21) })
            ).validate(AaregTestData.evigArbeidsForholdListe)
        }
    }

    @Test
    internal fun `Til dato kan ikke komme før fra dato`() {
        validationShouldFailFor("perioder[0].fom") {
            KroniskTestData.kroniskKravRequestValid.copy(
                perioder = listOf(
                    KroniskTestData.kroniskKravRequestValid.perioder.first().copy(
                        perioder = listOf(
                            Periode(
                                fom = LocalDate.of(2020, 1, 10),
                                tom = LocalDate.of(2020, 1, 5)
                            )
                        )
                    )
                        .also {
                            it.felter = it.felter.copy(antallDagerMedRefusjon = -5)
                        }
                )
            ) // slik at validationShouldFailFor() kaster ikke to unntak
                .validate(AaregTestData.evigArbeidsForholdListe)
        }
    }

    @Test
    internal fun `Sykemeldingsgrad må være gyldig`() {
        validationShouldFailFor("perioder[0].gradering") {
            KroniskTestData.kroniskKravRequestValid.copy(
                perioder = listOf(
                    KroniskTestData.kroniskKravRequestValid.perioder.first().copy().also {
                        it.felter = it.felter.copy(gradering = 1.1)
                    }
                )
            ).validate(AaregTestData.evigArbeidsForholdListe)
        }

        validationShouldFailFor("perioder[0].gradering") {
            KroniskTestData.kroniskKravRequestValid.copy(
                perioder = listOf(
                    KroniskTestData.kroniskKravRequestValid.perioder.first().copy().also {
                        it.felter = it.felter.copy(gradering = 0.1)
                    }
                )
            ).validate(AaregTestData.evigArbeidsForholdListe)
        }
    }
}
