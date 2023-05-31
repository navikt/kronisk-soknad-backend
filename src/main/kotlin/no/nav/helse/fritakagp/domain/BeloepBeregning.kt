package no.nav.helse.fritakagp.domain

import no.nav.helse.fritakagp.integration.GrunnbeloepClient
import java.math.BigDecimal
import java.math.RoundingMode

class BeloepBeregning(
    private val grunnbeloepClient: GrunnbeloepClient
) {
    fun beregnBeløpKronisk(krav: KroniskKrav) = beregnPeriodeData(krav.perioder, krav.antallDager)

    fun beregnBeløpGravid(krav: GravidKrav) = beregnPeriodeData(krav.perioder, krav.antallDager)

    private fun beregnPeriodeData(perioder: List<ArbeidsgiverperiodeNy>, antallDager: Int) {
        perioder.forEach {
            val seksG = grunnbeloepClient.hentGrunnbeløp(it.fraOgMed()).grunnbeløp * 6.0
            val arslonn = it.felter.månedsinntekt * 12
            it.felter.dagsats = if (arslonn < seksG)
                round2DigitDecimal(arslonn / antallDager)
            else
                round2DigitDecimal(seksG / antallDager)
            it.felter.belop = round2DigitDecimal(it.felter.dagsats * it.felter.antallDagerMedRefusjon * it.felter.gradering)
        }
    }

    private fun round2DigitDecimal(value: Double): Double = BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
}
