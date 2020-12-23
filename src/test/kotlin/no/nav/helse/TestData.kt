package no.nav.helse

import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.domain.OmplasseringAarsak
import no.nav.helse.fritakagp.domain.SoeknadGravid
import no.nav.helse.fritakagp.domain.Tiltak
import no.nav.helse.fritakagp.web.api.resreq.GravideSoknadRequest

object TestData {
    val validIdentitetsnummer = "20015001543"
    val validOrgNr = "123456785"

    val soeknadGravid = SoeknadGravid(
        orgnr = validOrgNr,
        fnr = validIdentitetsnummer,
        tilrettelegge = true,
        tiltak = listOf(Tiltak.HJEMMEKONTOR, Tiltak.ANNET),
        tiltakBeskrivelse = "Vi prøvde både det ene og det andre og det første kanskje virka litt men muligens and the andre ikke var så på stell men akk ja sånn lorem",
        omplassering = Omplassering.IKKE_MULIG,
        omplasseringAarsak = OmplasseringAarsak.HELSETILSTANDEN,
        sendtAv = "09876543210"
    )

    val fullValidRequest = GravideSoknadRequest(
        orgnr = validOrgNr,
        fnr = validIdentitetsnummer,
        tilrettelegge = true,
        tiltak = listOf(Tiltak.ANNET, Tiltak.HJEMMEKONTOR, Tiltak.TILPASSEDE_ARBEIDSOPPGAVER, Tiltak.TILPASSET_ARBEIDSTID),
        tiltakBeskrivelse = "beskrivelse",
        omplassering = Omplassering.NEI,
        omplasseringAarsak = OmplasseringAarsak.HELSETILSTANDEN,
        bekreftet = true
    )
}

