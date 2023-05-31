package no.nav.helse.fritakagp.domain

import no.nav.helse.fritakagp.customObjectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat

class ArbeidsgiverPeriodeJsonTest {

    val om = customObjectMapper()

    val periode = Periode(
        fom = LocalDate.of(2022, 4, 1),
        tom = LocalDate.of(2022, 4, 16)
    )

    val testStringLegacy: String = "{\n" +
        "  \"fom\" : \"2022-04-01\",\n" +
        "  \"tom\" : \"2022-04-16\",\n" +
        "  \"antallDagerMedRefusjon\" : 3,\n" +
        "  \"månedsinntekt\" : 3000.0,\n" +
        "  \"gradering\" : 1.0,\n" +
        "  \"dagsats\" : 0.0,\n" +
        "  \"belop\" : 0.0\n" +
        "}".trimIndent()

    val testStringNew: String = "{\n" +
        "  \"perioder\" : [ {\n" +
        "    \"fom\" : \"2022-04-01\",\n" +
        "    \"tom\" : \"2022-04-16\"\n" +
        "  } ],\n" +
        "  \"antallDagerMedRefusjon\" : 3,\n" +
        "  \"månedsinntekt\" : 3000.0,\n" +
        "  \"gradering\" : 1.0,\n" +
        "  \"dagsats\" : 0.0,\n" +
        "  \"belop\" : 0.0\n" +
        "}".trimIndent()

    val testStringBegge: String = "{\n" +
        "  \"fom\" : \"2022-04-01\",\n" +
        "  \"tom\" : \"2022-04-16\",\n" +
        "  \"perioder\" : [ {\n" +
        "    \"fom\" : \"2022-04-01\",\n" +
        "    \"tom\" : \"2022-04-16\"\n" +
        "  } ],\n" +
        "  \"antallDagerMedRefusjon\" : 3,\n" +
        "  \"månedsinntekt\" : 3000.0,\n" +
        "  \"gradering\" : 1.0,\n" +
        "  \"dagsats\" : 0.0,\n" +
        "  \"belop\" : 0.0\n" +
        "}".trimIndent()
    @Test
    fun testJsonBegge() {
        val resultatBegge = om.readValue(testStringBegge, ArbeidsgiverperiodeNy::class.java)
        assertThat(resultatBegge.perioder?.size).isEqualTo(1)
        assertThat(resultatBegge.perioder?.get(0)).isEqualTo(periode)
    }
    @Test
    fun testJsonNy() {
        val resultatNy = om.readValue(testStringNew, ArbeidsgiverperiodeNy::class.java)
        assertThat(resultatNy.perioder?.size).isEqualTo(1)
        assertThat(resultatNy.perioder?.get(0)).isEqualTo(periode)
    }
    @Test
    fun testJsonLegacy() {
        val resultatLegacy = om.readValue(testStringLegacy, ArbeidsgiverperiodeNy::class.java)
        assertThat(resultatLegacy.perioder?.size).isEqualTo(1)
        assertThat(resultatLegacy.perioder?.get(0)).isEqualTo(periode)
    }

    @Test
    fun testSerialization() {
        val om = customObjectMapper()

        val felter = AgpFelter(
            3,
            3000.0,
        )

        val testPeriodeLegacy = ArbeidsgiverperiodeNy(
            _fom = periode.fom,
            _tom = periode.tom,
            perioder = null,
        ).also { it.felter = felter }

        val testPeriodeNy = ArbeidsgiverperiodeNy(
            perioder = listOf(periode),
        ).also { it.felter = felter }

        // println(om.writeValueAsString(testPeriode))
        val tmpString = om.writeValueAsString(testPeriodeNy)
        println(tmpString)
        val resultStringLegacy = om.writeValueAsString(testPeriodeLegacy)
        println(resultStringLegacy)
    }
}
