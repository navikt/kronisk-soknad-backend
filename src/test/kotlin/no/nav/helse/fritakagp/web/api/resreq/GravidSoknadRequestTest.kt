package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.domain.OmplasseringAarsak
import no.nav.helse.fritakagp.domain.Tiltak
import org.junit.jupiter.api.Test

class GravidSoknadRequestTest{

    @Test
    internal fun `Gyldig FNR er påkrevd`() {
        validationShouldFailFor(GravidSoknadRequest::fnr) {
            GravidTestData.fullValidRequest.copy(fnr = "01020312345").validate()
        }
    }

    @Test
    internal fun `Gyldig OrgNr er påkrevd dersom det er oppgitt`() {
        validationShouldFailFor(GravidSoknadRequest::orgnr) {
            GravidTestData.fullValidRequest.copy(orgnr = "098765432").validate()
        }
    }

    @Test
    fun `Når tiltak inneholder ANNET må tiltaksbeskrivelse ha innhold`() {
        validationShouldFailFor(GravidSoknadRequest::tiltakBeskrivelse) {
            GravidTestData.fullValidRequest.copy(
                tiltak = listOf(Tiltak.ANNET),
                tiltakBeskrivelse = "",
            ).validate()
        }

        GravidTestData.fullValidRequest.copy(
            tiltak = listOf(Tiltak.ANNET),
            tiltakBeskrivelse = "dette går bra",
        ).validate()
    }

    @Test
    fun `Om tiltak ikke inneholder ANNET er ikke tiltaksbeskrivelse påkrevd`() {
        GravidTestData.fullValidRequest.copy(
            tiltak = listOf(Tiltak.TILPASSET_ARBEIDSTID),
            tiltakBeskrivelse = null
        ).validate()
    }

    @Test
    internal fun `Bekreftelse av egenerklæring er påkrevd`() {
        validationShouldFailFor(GravidSoknadRequest::bekreftet) {
            GravidTestData.fullValidRequest.copy(bekreftet = false).validate()
        }
    }

    @Test
    fun `Dersom omplassering ikke er mulig må det finnes en årsak`() {
        validationShouldFailFor(GravidSoknadRequest::omplasseringAarsak) {
            GravidTestData.fullValidRequest.copy(
                omplassering = Omplassering.IKKE_MULIG,
                omplasseringAarsak = null
            ).validate()
        }

        GravidTestData.fullValidRequest.copy(
            omplassering = Omplassering.IKKE_MULIG,
            omplasseringAarsak = OmplasseringAarsak.FAAR_IKKE_KONTAKT
        ).validate()
    }
}