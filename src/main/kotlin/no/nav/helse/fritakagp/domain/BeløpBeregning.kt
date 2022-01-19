package no.nav.helse.fritakagp.domain

import no.nav.helse.fritakagp.integration.GrunnbeløpClient
import java.math.BigDecimal
import java.math.RoundingMode

class BeløpBeregning(
    grunnbeløpClient: GrunnbeløpClient
) {
    private val seksG = grunnbeløpClient.hentGrunnbeløp().grunnbeløp * 6.0

    fun beregnBeløpKronisk(krav: KroniskKrav) = beregnPeriodeData(krav.perioder, krav.antallDager)

    fun beregnBeløpGravid(krav: GravidKrav) = beregnPeriodeData(krav.perioder, krav.antallDager)

    private fun beregnPeriodeData(perioder: List<Arbeidsgiverperiode>, antallDager: Int) {
        return beregnPeriodeDataSeksG(perioder, antallDager, seksG)
    }
}

fun beregnPeriodeDataSeksG(perioder: List<Arbeidsgiverperiode>, antallDager: Int, seksG: Double) {
    perioder.forEach {
        val arslonn = it.månedsinntekt * 12
        it.dagsats = if (arslonn < seksG)
            round2DigitDecimal(if (antallDager == 0) 0.0 else (arslonn / antallDager))
        else
            round2DigitDecimal(if (antallDager == 0) 0.0 else (seksG / antallDager))
        it.belop = round2DigitDecimal(it.dagsats * it.antallDagerMedRefusjon * it.gradering)
    }
}

fun round2DigitDecimal(value: Double): Double = BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
