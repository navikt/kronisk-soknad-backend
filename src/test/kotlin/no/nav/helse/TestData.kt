package no.nav.helse

import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.domain.SoeknadGravid
import no.nav.helse.fritakagp.domain.Tiltak

object TestData {
    val soeknadGravid = SoeknadGravid(
        orgnr = "123456785",
        fnr = "20015001543",
        tilrettelegge = true,
        tiltak = listOf(Tiltak.HJEMMEKONTOR, Tiltak.ANNET),
        tiltakBeskrivelse = "Vi prøvde både det ene og det andre og det første kanskje virka litt men muligens and the andre ikke var så på stell men akk ja sånn lorem",
        omplassering = Omplassering.IKKE_MULIG.toString(),
        omplasseringAarsak = "Vi får ikke kontakt med den ansatte",
        sendtAv = "09876543210"
    )
}

