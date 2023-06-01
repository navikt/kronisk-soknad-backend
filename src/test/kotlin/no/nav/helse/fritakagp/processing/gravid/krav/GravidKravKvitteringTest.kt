package no.nav.helse.fritakagp.processing.gravid.krav

import no.nav.helse.GravidTestData
import org.junit.jupiter.api.Test

class GravidKravKvitteringTest {

    @Test
    fun `innholdet i kvitteringen er riktig`() {
        val innhold = lagInnhold(GravidTestData.gravidKrav)
        println(innhold)
    }
}
