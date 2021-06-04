package no.nav.helse.fritakagp.domain

import no.nav.helse.fritakagp.integration.GrunnbeløpClient

class BeløpBeregning(
    grunnbeløpClient: GrunnbeløpClient
) {
    private val seksG = grunnbeløpClient.hentGrunnbeløp().grunnbeløp * 6.0

    fun beregnBeløpKronisk(krav : KroniskKrav) {
        val arslonn = krav.månedsinntekt * 12
        beregnPeriodeData(krav.perioder, krav.antallDager, arslonn)

    }


    fun beregnBeløpGravid(krav : GravidKrav) {
        val arslonn = krav.månedsinntekt * 12
        beregnPeriodeData(krav.perioder, krav.antallDager, arslonn)
    }

    private fun beregnPeriodeData(perioder: Set<Arbeidsgiverperiode>, antallDager: Int, arslonn: Double) {
        perioder.forEach {
            it.dagsats = if (arslonn < seksG)
                arslonn / antallDager
            else
                seksG / antallDager
            it.belop = it.dagsats * it.antallDagerMedRefusjon
        }
    }

}