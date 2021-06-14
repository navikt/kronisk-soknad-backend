package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.KroniskTestData
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KroniskKravRequestTest{

    @Test
    internal fun `Gyldig FNR er påkrevd`() {
        validationShouldFailFor(KroniskKravRequest::identitetsnummer) {
            KroniskTestData.kroniskKravRequestValid.copy(identitetsnummer = "01020312345").validate()
        }
    }

    @Test
    internal fun `Gyldig OrgNr er påkrevd dersom det er oppgitt`() {
        validationShouldFailFor(KroniskKravRequest::virksomhetsnummer) {
            KroniskTestData.kroniskKravRequestValid.copy(virksomhetsnummer = "098765432").validate()
        }
    }

    @Test
    internal fun `Bekreftelse av egenerklæring er påkrevd`() {
        validationShouldFailFor(KroniskKravRequest::bekreftet) {
            KroniskTestData.kroniskKravRequestValid.copy(bekreftet = false).validate()
        }
    }

    @Test
    internal fun `mapping til domenemodell tar med harVedleggflagg`() {
        Assertions.assertThat(KroniskTestData.kroniskKravRequestMedFil.toDomain("123").harVedlegg).isTrue
        Assertions.assertThat(KroniskTestData.kroniskKravRequestValid.toDomain("123").harVedlegg).isFalse
    }

    @Test
    internal fun `Antall refusjonsdager kan ikke overstige periodelengden`() {
        validationShouldFailFor(KroniskKravRequest::perioder) {
            KroniskTestData.kroniskKravRequestValid.copy(
                perioder = setOf(KroniskTestData.kroniskKravRequestValid.perioder.first().copy(antallDagerMedRefusjon = 21))
            ).validate()
        }
    }

    @Test
    internal fun `Til dato kan ikke komme før fra dato`() {
        validationShouldFailFor(KroniskKravRequest::perioder) {
            KroniskTestData.kroniskKravRequestValid.copy(
                perioder = setOf(KroniskTestData.kroniskKravRequestValid.perioder.first().copy(fom = LocalDate.of(2020, 1, 10),
                    tom = LocalDate.of(2020, 1, 5),
                antallDagerMedRefusjon = -5)) //slik at validationShouldFailFor() kaster ikke to unntak
            ).validate()
        }
    }
}