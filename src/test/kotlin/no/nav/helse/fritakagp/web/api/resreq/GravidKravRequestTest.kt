package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.GravidTestData
import org.junit.jupiter.api.Test

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
    internal fun `Antall refusjonsdager kan ikke overstige periodelengden`() {
        validationShouldFailFor(GravidKravRequest::periode) {
            GravidTestData.gravidKravRequestValid.copy(
                periode = GravidTestData.gravidKravRequestValid.periode.copy(antallDagerMedRefusjon = 21)
            ).validate()
        }
    }
}