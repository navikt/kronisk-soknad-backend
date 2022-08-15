package no.nav.helse.fritakagp.integration

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.SimpleHashMapCache
import java.time.Duration
import java.time.LocalDate

class GrunnbeloepClient(
    private val httpClient: HttpClient,
) {
    private val cache = SimpleHashMapCache<GrunnbeløpInfo>(Duration.ofDays(1), 5)

    fun hentGrunnbeløp(dato: LocalDate): GrunnbeløpInfo {
        val cacheKey = if (dato.month.value >= 5) "${dato.year}-05" else "${dato.year - 1}-05"

        return cache.use(cacheKey) {
            runBlocking {
                httpClient.get {
                    url("https://g.nav.no/api/v1/grunnbeløp?dato=$dato")
                }
            }
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
