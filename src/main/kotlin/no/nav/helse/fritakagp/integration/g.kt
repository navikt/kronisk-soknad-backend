package no.nav.helse.fritakagp.integration

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.SimpleHashMapCache
import java.time.Duration
import java.time.LocalDate

class GrunnbeløpClient(val httpClient: HttpClient) {
    fun hentGrunnbeløp(dato: LocalDate): GrunnbeløpInfo = runBlocking {
        httpClient.get<GrunnbeløpInfo> {
            url("https://g.nav.no/api/v1/grunnbeløp?dato=$dato")
        }
    }
}

/**
 * {
"dato": "2020-05-01",
"grunnbeløp": 101351,
"grunnbeløpPerMåned": 8446,
"gjennomsnittPerÅr": 100853,
"omregningsfaktor": 1.014951
}
 */
data class GrunnbeløpInfo(
    val dato: LocalDate,
    val grunnbeløp: Int,
    val grunnbeløpPerMåned: Int,
    val gjennomsnittPerÅr: Int,
    val omregningsfaktor: Double
)
