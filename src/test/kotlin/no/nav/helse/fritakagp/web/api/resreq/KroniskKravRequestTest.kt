package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.AaregTestData
import no.nav.helse.KroniskTestData
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KroniskKravRequestTest{

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
        Assertions.assertThat(KroniskTestData.kroniskKravRequestMedFil.toDomain("123").harVedlegg).isTrue
        Assertions.assertThat(KroniskTestData.kroniskKravRequestValid.toDomain("123").harVedlegg).isFalse
    }

    @Test
    internal fun `Antall refusjonsdager kan ikke overstige periodelengden`() {
        validationShouldFailFor("perioder[0].antallDagerMedRefusjon") {
            KroniskTestData.kroniskKravRequestValid.copy(
                perioder = listOf(KroniskTestData.kroniskKravRequestValid.perioder.first().copy(antallDagerMedRefusjon = 21))
            ).validate(AaregTestData.evigArbeidsForholdListe)
        }
    }

    @Test
    internal fun `Til dato kan ikke komme før fra dato`() {
        validationShouldFailFor("perioder[0].fom") {
            KroniskTestData.kroniskKravRequestValid.copy(
                perioder = listOf(KroniskTestData.kroniskKravRequestValid.perioder.first().copy(fom = LocalDate.of(2020, 1, 10),
                    tom = LocalDate.of(2020, 1, 5),
                antallDagerMedRefusjon = -5)) //slik at validationShouldFailFor() kaster ikke to unntak
            ).validate(AaregTestData.evigArbeidsForholdListe)
        }
    }
}