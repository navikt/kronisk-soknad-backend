package no.nav.helse.fritakagp.domain

import java.time.LocalDate

data class Arbeidsgiverperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallDagerMedRefusjon: Int,
    val månedsinntekt: Double,
    var index: Int = 0
){
    var dagsats : Double = 0.0
    var belop: Double = 0.0
}