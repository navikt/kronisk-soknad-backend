package no.nav.helse.fritakagp.domain

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

internal class BeløpBeregningTest {

    @Test
    fun skal_håndtere_0_dager() {
        org.junit.jupiter.api.assertDoesNotThrow { beregnPeriodeDataSeksG(mockPerioder(), 0, 5000.0) }
    }

    @Test
    fun skal_håndtere_1_dager() {
        org.junit.jupiter.api.assertDoesNotThrow { beregnPeriodeDataSeksG(mockPerioder(), 1, 5000.0) }
    }

    fun mockPerioder(): ArrayList<Arbeidsgiverperiode> {
        val perioder: ArrayList<Arbeidsgiverperiode> = arrayListOf()
        perioder.add(Arbeidsgiverperiode(LocalDate.now(), LocalDate.now(), 0, 0.0, 1.0))
        return perioder
    }
}
