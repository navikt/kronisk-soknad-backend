package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.GravidTestData
import no.nav.helse.KroniskTestData
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

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
        Assertions.assertThat(KroniskTestData.kroniskKravRequestMedFil.toDomain("123", "Stark Industries").harVedlegg).isTrue()
        Assertions.assertThat(KroniskTestData.kroniskKravRequestValid.toDomain("123", "Stark Industries").harVedlegg).isFalse()
    }

    @Test
    internal fun `Antall refusjonsdager kan ikke overstige periodelengden`() {
        validationShouldFailFor(KroniskKravRequest::perioder) {
            KroniskTestData.kroniskKravRequestValid.copy(
                perioder = setOf(KroniskTestData.kroniskKravRequestValid.perioder.first().copy(antallDagerMedRefusjon = 21))
            ).validate()
        }
    }
}