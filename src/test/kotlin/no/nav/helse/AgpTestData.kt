package no.nav.helse

import no.nav.helse.fritakagp.domain.AgpFelter
import no.nav.helse.fritakagp.domain.ArbeidsgiverperiodeNy
import no.nav.helse.fritakagp.domain.Periode
import java.time.LocalDate

object AgpTestData {
    val arbeidsgiverperiode = ArbeidsgiverperiodeNy(
        perioder = listOf(
            Periode(
                LocalDate.of(2020, 1, 15),
                LocalDate.of(2020, 1, 10)
            )
        ),
        antallDagerMedRefusjon = 2, månedsinntekt = 2590.8
    )
}
