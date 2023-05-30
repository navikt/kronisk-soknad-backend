package no.nav.helse.fritakagp.domain

import no.nav.helse.fritakagp.customObjectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat


class ArbeidsgiverPeriodeJsonTest{

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
    @Test
    fun testJson(){
        val om = customObjectMapper()

        val felter = AgpFelter(
            3,
            3000.0,
        )

        val periode = Periode(
            fom = LocalDate.of(2022, 4, 1),
            tom = LocalDate.of(2022, 4, 16)
        )

        val testPeriodeLegacy = ArbeidsgiverperiodeLegacy(
            periode = periode,
            felter = felter
        )

        val testPeriodeNy = ArbeidsgiverperiodeNy(
            perioder = listOf(periode),
            felter = felter
        )

        //println(om.writeValueAsString(testPeriode))
        val tmpString = om.writeValueAsString(testPeriodeNy)
        println(tmpString)
        val resultStringLegacy = om.writeValueAsString(testPeriodeLegacy)
        println(resultStringLegacy)

        //val resultat =  om.readValue(tmpString, Arbeidsgiverperiode3::class.java)
        //val resultat =  om.readValue(testStringNew, Arbeidsgiverperiode3::class.java)
        val resultat =  om.readValue(testStringLegacy, ArbeidsgiverperiodeNy::class.java)

        assertThat(resultat.perioder.size).isEqualTo(1)
    }


}
