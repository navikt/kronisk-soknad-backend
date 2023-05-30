package no.nav.helse.fritakagp.domain

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.LocalDate

data class AgpFelter(
    val antallDagerMedRefusjon: Int,
    val månedsinntekt: Double,
    val gradering: Double = 1.0,
) {
    var dagsats: Double = 0.0
    var belop: Double = 0.0
}



//TODO erstatt med ArbeidsgiverperiodeNy
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


// Gammel Modell med gjenbruk (skal bare brukes for robot oppgaver)
data class ArbeidsgiverperiodeLegacy(
    @field:JsonUnwrapped
    val periode: Periode,
    @field:JsonUnwrapped
    val felter: AgpFelter
)


// Ny model
@JsonDeserialize(using = ArbeidsgiverperiodeConversions.Deserializer::class)
data class ArbeidsgiverperiodeNy(
    val perioder: List<Periode>,
    @field:JsonUnwrapped
    var felter: AgpFelter
)

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
)
object ArbeidsgiverperiodeConversions {
    object Deserializer : JsonDeserializer<ArbeidsgiverperiodeNy>() {
        override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): ArbeidsgiverperiodeNy {
            val node = parser.readValueAsTree<JsonNode>()

            val antallDagerMedRefusjon = node.get("antallDagerMedRefusjon").asInt()
            val månedsinntekt = node.get("månedsinntekt").asDouble()
            val gradering = node.get("gradering").asDouble()
            val dagsats = node.get("dagsats").asDouble()
            val belop = node.get("belop").asDouble()

            val felter = AgpFelter(
                antallDagerMedRefusjon = antallDagerMedRefusjon,
                månedsinntekt = månedsinntekt,
                gradering = gradering,
            )

            felter.dagsats = dagsats
            felter.belop = belop

            val perioder = node.get("perioder")?.let{ perioder ->
                    perioder.asSequence().toList().map { p ->
                        Periode(
                            p.get("fom").asText().let { LocalDate.parse(it) },
                            p.get("tom").asText().let { LocalDate.parse(it) },
                        )
                    }
            } ?: run {
                val fom = node.get("fom").asText().let { LocalDate.parse(it) }
                val tom = node.get("tom").asText().let { LocalDate.parse(it) }
                listOf(Periode(fom, tom))
            }
            return ArbeidsgiverperiodeNy(perioder, felter)
        }
    }
}
