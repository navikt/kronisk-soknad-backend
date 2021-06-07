package no.nav.helse.fritakagp.domain

import no.nav.helse.fritakagp.integration.GrunnbeløpClient

class BeløpBeregning(
    grunnbeløpClient: GrunnbeløpClient
) {
    private val seksG = grunnbeløpClient.hentGrunnbeløp().grunnbeløp * 6.0

    fun beregnBeløpKronisk(krav : KroniskKrav)  = beregnPeriodeData(krav.perioder, krav.antallDager)

    fun beregnBeløpGravid(krav : GravidKrav) = beregnPeriodeData(krav.perioder, krav.antallDager)

    private fun beregnPeriodeData(perioder: Set<Arbeidsgiverperiode>, antallDager: Int) {
        perioder.forEach {
            val arslonn = it.månedsinntekt * 12
            it.dagsats = if (arslonn < seksG)
                arslonn / antallDager
            else
                seksG / antallDager
            it.belop = it.dagsats * it.antallDagerMedRefusjon
        }
    }
}