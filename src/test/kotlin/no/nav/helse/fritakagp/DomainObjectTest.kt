package no.nav.helse.fritakagp

import no.nav.helse.mockGravidKrav
import no.nav.helse.mockGravidSoeknad
import no.nav.helse.mockKroniskKrav
import no.nav.helse.mockKroniskSoeknad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DomainObjectTest {
    @Test
    fun `GravidSoeknad autogenererer ulike ID-er`() {
        val id1 = mockGravidSoeknad().id
        val id2 = mockGravidSoeknad().id

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `GravidKrav autogenererer ulike ID-er`() {
        val id1 = mockGravidKrav().id
        val id2 = mockGravidKrav().id

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `KroniskSoeknad autogenererer ulike ID-er`() {
        val id1 = mockKroniskSoeknad().id
        val id2 = mockKroniskSoeknad().id

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `KroniskKrav autogenererer ulike ID-er`() {
        val id1 = mockKroniskKrav().id
        val id2 = mockKroniskKrav().id

        assertThat(id1).isNotEqualTo(id2)
    }
}
