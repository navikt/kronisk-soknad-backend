package no.nav.helse.fritakagp.domain

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import java.time.LocalDate

data class AgpFelter(
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
// _fom og _tom kan antageligvis slettes etter 6 måneder hvis sletting av gamle data fungerer
//@JsonDeserialize(using = ArbeidsgiverperiodeConversions.Deserializer::class)
data class ArbeidsgiverperiodeNy(
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("fom")
    private var _fom: LocalDate? = null,
    @JsonProperty("tom")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private var _tom: LocalDate? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    var perioder: List<Periode>?,
){

    init {
        if (perioder.isNullOrEmpty()) perioder  = listOf(Periode(_fom!!, _tom!!))
        _fom = null
        _tom = null
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

