package no.nav.helse.fritakagp.integration

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.SimpleHashMapCache
import java.time.Duration
import java.time.LocalDate

class GrunnbeløpClient(val httpClient: HttpClient) {
    val cache = SimpleHashMapCache<GrunnbeløpInfo>(Duration.ofDays(1), 5)

    fun hentGrunnbeløp(dato: LocalDate): GrunnbeløpInfo {
        val cacheKey = if (dato.month.value > 5) "${dato.year}-05" else "${dato.year - 1}-05"
        if (cache.hasValidCacheEntry(cacheKey)) return cache.get(cacheKey)
        val g = runBlocking {
            httpClient.get<GrunnbeløpInfo> {
                url("https://g.nav.no/api/v1/grunnbeløp?dato=$dato")
            }
        }
        cache.put(cacheKey, g)
        return g
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
