package no.nav.helse.fritakagp.web

import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.domain.OmplasseringAarsak
import no.nav.helse.fritakagp.domain.Tiltak
import no.nav.helse.fritakagp.web.api.resreq.GravideSoknadRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ValidateRequestTest
{
    @Test
    fun `test tiltakbeskrivelse`() {
        GravideSoknadRequest(
                orgnr = "123456785",
                fnr = "20015001543",
                tilrettelegge = true,
                tiltak = listOf(Tiltak.ANNET),
                tiltakBeskrivelse = "beskrivelse",
                omplassering= Omplassering.JA,
                omplasseringAarsak = null
        )
    }

    @Test
    fun `test tiltakbeskrivelse uten beskrivelse`() {
        Assertions.assertThrows(org.valiktor.ConstraintViolationException::class.java) {
            GravideSoknadRequest(
                orgnr = "123456785",

                fnr = "20015001543",
                    tilrettelegge = true,
                    tiltak = listOf(Tiltak.ANNET),
                    tiltakBeskrivelse = "",
                    omplassering= Omplassering.JA,
                    omplasseringAarsak = null
            )
        }
    }

    @Test
    fun `test tiltakbeskrivelse uten Annet`() {
        GravideSoknadRequest(
            orgnr = "123456785",

            fnr = "20015001543",
                tilrettelegge = true,
                tiltak = listOf(Tiltak.TILPASSET_ARBEIDSTID),
                tiltakBeskrivelse = "",
                omplassering= Omplassering.JA,
                omplasseringAarsak = null
        )
    }

    @Test
    fun `test omplassering valg`() {
        GravideSoknadRequest(
            orgnr = "123456785",

            fnr = "20015001543",
                tilrettelegge = true,
                tiltak = listOf(Tiltak.TILPASSET_ARBEIDSTID),
                tiltakBeskrivelse = "",
                omplassering= Omplassering.IKKE_MULIG,
                omplasseringAarsak = OmplasseringAarsak.FAAR_IKKE_KONTAKT
        )
    }
}