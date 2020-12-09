package no.nav.helse

import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.domain.OmplasseringAarsak
import no.nav.helse.fritakagp.domain.SoeknadGravid
import no.nav.helse.fritakagp.domain.Tiltak
import no.nav.helse.fritakagp.web.api.resreq.GravideSoknadRequest

object TestData {
    val validIdentitetsnummer = "20015001543"
    val soeknadGravid = SoeknadGravid(
        orgnr = "123456785",
        fnr = validIdentitetsnummer,
        tilrettelegge = true,
        tiltak = listOf(Tiltak.HJEMMEKONTOR, Tiltak.ANNET),
        tiltakBeskrivelse = "Vi prøvde både det ene og det andre og det første kanskje virka litt men muligens and the andre ikke var så på stell men akk ja sånn lorem",
        omplassering = Omplassering.IKKE_MULIG,
        omplasseringAarsak = OmplasseringAarsak.HELSETILSTANDEN,
        sendtAv = "09876543210"
    )

    val validRequest = GravideSoknadRequest(
        orgnr = "123456785",
        fnr = "20015001543",
        tilrettelegge = true,
        tiltak = listOf(Tiltak.ANNET),
        tiltakBeskrivelse = "beskrivelse",
        omplassering = Omplassering.JA,
        omplasseringAarsak = null
    )
}

