package no.nav.helse.fritakagp.domain

import no.nav.helse.fritakagp.integration.GrunnbeløpClient

class BeløpBeregning(
    val grunnbeløpClient: GrunnbeløpClient
) {
    val seksG = grunnbeløpClient.hentGrunnbeløp().grunnbeløp * 6.0

    fun beregnBeløpKronisk(krav : KroniskKrav) {
        krav.perioder.forEach {
            val arslonn = it.månedsinntekt * 12
            it.dagsats = if (arslonn < seksG)
                arslonn / krav.antallDager
            else
                seksG / krav.antallDager
            it.belop = it.dagsats * it.antallDagerMedRefusjon
        }
    }
    fun beregnBeløpGravid(krav : GravidKrav) {
        krav.perioder.forEach {
            val arslonn = it.månedsinntekt * 12
            it.dagsats = if (arslonn < seksG)
                arslonn / krav.antallDager
            else
                seksG / krav.antallDager
            it.belop = it.dagsats * it.antallDagerMedRefusjon
        }
    }

}