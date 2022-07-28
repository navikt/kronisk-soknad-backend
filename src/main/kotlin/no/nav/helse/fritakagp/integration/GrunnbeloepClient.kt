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
    private val cache = SimpleHashMapCache<GrunnbeløpInfo>(Duration.ofDays(1), 2)

    fun hentGrunnbeløp(): GrunnbeløpInfo =
        cache.use("g") {
            runBlocking {
                httpClient.get {
                    url("https://g.nav.no/api/v1/grunnbeløp")
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
