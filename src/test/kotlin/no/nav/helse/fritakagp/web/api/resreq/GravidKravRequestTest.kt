package no.nav.helse.fritakagp.web.api.resreq

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.GravidTestData
import no.nav.helse.KroniskTestData
import no.nav.helse.fritakagp.domain.BeløpBeregning
import no.nav.helse.fritakagp.integration.GrunnbeløpClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GravidKravRequestTest{

    @Test
    internal fun `Gyldig FNR er påkrevd`() {
        validationShouldFailFor(GravidKravRequest::identitetsnummer) {
            GravidTestData.gravidKravRequestValid.copy(identitetsnummer = "01020312345").validate()
        }
    }

    @Test
    internal fun `Gyldig OrgNr er påkrevd dersom det er oppgitt`() {
        validationShouldFailFor(GravidKravRequest::virksomhetsnummer) {
            GravidTestData.gravidKravRequestValid.copy(virksomhetsnummer = "098765432").validate()
        }
    }

    @Test
    internal fun `Bekreftelse av egenerklæring er påkrevd`() {
        validationShouldFailFor(GravidKravRequest::bekreftet) {
            GravidTestData.gravidKravRequestValid.copy(bekreftet = false).validate()
        }
    }

    @Test
    internal fun `mapping til domenemodell tar med harVedleggflagg`() {
        assertThat(GravidTestData.gravidKravRequestMedFil.toDomain("123").harVedlegg).isTrue
        assertThat(GravidTestData.gravidKravRequestValid.toDomain("123").harVedlegg).isFalse

    }

    @Test
    internal fun `Antall refusjonsdager kan ikke overstige periodelengden`() {
        validationShouldFailFor(GravidKravRequest::perioder) {
            GravidTestData.gravidKravRequestValid.copy(
                perioder = setOf(GravidTestData.gravidKravRequestValid.perioder.first().copy(antallDagerMedRefusjon = 21))
            ).validate()
        }
    }

    @Test
    internal fun `Til dato kan ikke komme før fra dato`() {
        validationShouldFailFor(GravidKravRequest::perioder) {
            GravidTestData.gravidKravRequestValid.copy(
                perioder = setOf(GravidTestData.gravidKravRequestValid.perioder.first().copy(fom = LocalDate.of(2020, 1, 10),
                    tom = LocalDate.of(2020, 1, 5),
                    antallDagerMedRefusjon = -5)) //slik at validationShouldFailFor() kaster ikke to unntak
            ).validate()
        }
    }

    @Test
    fun `Beløp og dagsats er beregnet`() {
        val grunnbeløpClient = mockk<GrunnbeløpClient>(relaxed = true)
        every { grunnbeløpClient.hentGrunnbeløp().grunnbeløp } returns 106399

        val belopBeregning =  BeløpBeregning(grunnbeløpClient)
        val krav = GravidTestData.gravidKravRequestValid.toDomain("123")
        belopBeregning.beregnBeløpGravid(krav)

        assertThat(krav.perioder.first().dagsats).isEqualTo(7772.4)
        assertThat(krav.perioder.first().belop).isEqualTo(15544.8)
    }
}