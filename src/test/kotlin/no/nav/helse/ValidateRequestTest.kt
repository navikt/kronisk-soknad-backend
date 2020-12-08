package no.nav.helse

import no.nav.helse.fritakagp.domain.OmplasseringAarsak
import no.nav.helse.fritakagp.domain.Tiltak
import no.nav.helse.fritakagp.web.api.resreq.GravideSoknadRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ValidateRequestTest {
    @Test
    fun `test tiltakbeskrivelse`() {
        GravideSoknadRequest(
            dato = LocalDate.now(),
            fnr = "20015001543",
            tilrettelegge = true,
            tiltak = listOf(Tiltak.ANNET.name),
            tiltakBeskrivelse = "beskrivelse",
            omplassering = "Ja",
            omplasseringAarsak = null,
            datafil = null,
            ext = null
        )
    }

    @Test
    fun `test tiltakbeskrivelse uten beskrivelse`() {
        Assertions.assertThrows(org.valiktor.ConstraintViolationException::class.java) {
            GravideSoknadRequest(
                dato = LocalDate.now(),
                fnr = "20015001543",
                tilrettelegge = true,
                tiltak = listOf(Tiltak.ANNET.name),
                tiltakBeskrivelse = "",
                omplassering = "Ja",
                omplasseringAarsak = null,
                datafil = null,
                ext = null
            )
        }
    }

    @Test
    fun `test tiltakbeskrivelse uten Annet`() {
        GravideSoknadRequest(
            dato = LocalDate.now(),
            fnr = "20015001543",
            tilrettelegge = true,
            tiltak = listOf(Tiltak.TILPASSET_ARBEIDSTID.name),
            tiltakBeskrivelse = "",
            omplassering = "Ja",
            omplasseringAarsak = null,
            datafil = null,
            ext = null
        )
    }

    @Test
    fun `test omplassering valg`() {
        GravideSoknadRequest(
            dato = LocalDate.now(),
            fnr = "20015001543",
            tilrettelegge = true,
            tiltak = listOf(Tiltak.TILPASSET_ARBEIDSTID.name),
            tiltakBeskrivelse = "",
            omplassering = "IKKE_MULIG",
            omplasseringAarsak = OmplasseringAarsak.FAAR_IKKE_KONTAKT.name,
            datafil = null,
            ext = null
        )
    }
}