package no.nav.helse.fritakagp.domain

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import java.time.LocalDate

class AgpFelter(
    val antallDagerMedRefusjon: Int,
    val månedsinntekt: Double,
    val gradering: Double = 1.0,
) {
    var dagsats: Double = 0.0
    var belop: Double = 0.0
}

data class Arbeidsgiverperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallDagerMedRefusjon: Int,
    val månedsinntekt: Double,
    val gradering: Double = 1.0
) {
    var dagsats: Double = 0.0
    var belop: Double = 0.0
}

// Ny model
//@JsonDeserialize(using = ArbeidsgiverperiodeConversions.Deserializer::class)
data class ArbeidsgiverperiodeNy(
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("fom")
    private val _fom: LocalDate? = null,
    @JsonProperty("tom")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private val _tom: LocalDate? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    var perioder: List<Periode>?,
){

    init {
        if (perioder.isNullOrEmpty()) perioder  = listOf(Periode(_fom!!, _tom!!))
    }
    @JsonUnwrapped
    lateinit var felter: AgpFelter

    @get:JsonInclude(JsonInclude.Include.NON_NULL)

    val fom get() = this._fom
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val tom get() = this._tom

    fun tilArbeidsgiverperideLegacy(): List<Arbeidsgiverperiode>? {
        return perioder?.map { Arbeidsgiverperiode(
            fom = it.fom,
            tom = it.tom,
            antallDagerMedRefusjon = felter.antallDagerMedRefusjon,
            månedsinntekt =  felter.månedsinntekt,
        ).also {
            it.belop = felter.belop
            it.dagsats = felter.dagsats
        } }?.toList()
    }

}

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
)

