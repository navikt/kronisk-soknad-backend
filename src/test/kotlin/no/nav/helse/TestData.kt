package no.nav.helse

import no.nav.helse.fritakagp.domain.Tiltak
import no.nav.helse.fritakagp.web.api.resreq.GravideSoknadRequest
import java.time.LocalDate

object TestData {
    val validIdentitetsnummer = "20015001543"
    val notValidIdentitetsnummer = "50012001987"
    val validOrgNr = "123456785"
    val notValidOrgNr = "123456789"
    val gravidSoknad = GravideSoknadRequest(
            dato = LocalDate.now(),
            fnr = "20015001543",
            tilrettelegge = true,
            tiltak = listOf(Tiltak.ANNET.name),
            tiltakBeskrivelse = "",
            omplassering = "Ja",
            omplasseringAarsak = null
    )
}