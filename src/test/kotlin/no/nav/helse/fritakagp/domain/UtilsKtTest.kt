package no.nav.helse.fritakagp.domain

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class UtilsKtTest {

    @Test
    fun sladderDeFemSisteSiffreneIFnr() {
        val FNR = "14070949904"
        val SLADDET_FNR = "140709*****"
        assertEquals(sladdFnr(FNR), SLADDET_FNR)
    }
}
