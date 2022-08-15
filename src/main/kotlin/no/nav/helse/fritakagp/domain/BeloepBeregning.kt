package no.nav.helse.fritakagp.domain

import no.nav.helse.fritakagp.integration.GrunnbeloepClient
import java.math.BigDecimal
import java.math.RoundingMode

class BeloepBeregning(
    private val grunnbeloepClient: GrunnbeloepClient
) {
    fun beregnBeløpKronisk(krav: KroniskKrav) = beregnPeriodeData(krav.perioder, krav.antallDager)

    fun beregnBeløpGravid(krav: GravidKrav) = beregnPeriodeData(krav.perioder, krav.antallDager)

    private fun beregnPeriodeData(perioder: List<Arbeidsgiverperiode>, antallDager: Int) {
        perioder.forEach {
            val seksG = grunnbeloepClient.hentGrunnbeløp(it.fom).grunnbeløp * 6.0
            val arslonn = it.månedsinntekt * 12
            it.dagsats = if (arslonn < seksG)
                round2DigitDecimal(arslonn / antallDager)
            else
                round2DigitDecimal(seksG / antallDager)
            it.belop = round2DigitDecimal(it.dagsats * it.antallDagerMedRefusjon * it.gradering)
        }
    }

    private fun round2DigitDecimal(value: Double): Double = BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
}
