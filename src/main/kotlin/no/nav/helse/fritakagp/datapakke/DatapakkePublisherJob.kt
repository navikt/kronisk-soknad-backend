package no.nav.helse.fritakagp.datapakke

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.helse.fritakagp.db.IStatsRepo
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime

// Todo: Gjør om til datafortelling
class DatapakkePublisherJob(
    private val statsRepo: IStatsRepo,
    private val httpClient: HttpClient,
    private val datapakkeApiUrl: String,
    private val datapakkeId: String,
    private val om: ObjectMapper,
    private val applyWeeklyOnly: Boolean = false
) :
    RecurringJob(
        CoroutineScope(Dispatchers.IO),
        Duration.ofHours(3).toMillis()
    ) {
    override fun doJob() {
        val now = LocalDateTime.now()
        if (applyWeeklyOnly && now.dayOfWeek != DayOfWeek.MONDAY && now.hour != 0) {
            return // Ikke kjør jobben med mindre det er natt til mandag
        }

        val datapakkeTemplate = "datapakke/datapakke-fritak.json".loadFromResources()

        val timeseries = statsRepo.getWeeklyStats()
        val gravidSoeknadTiltak = statsRepo.getGravidSoeknadTiltak()
        val sykegrad = statsRepo.getSykeGradAntall()

        val populatedDatapakke = datapakkeTemplate
            .replace(
                "@timeseries",
                timeseries.map { //language=JSON
                    """[${it.uke}, ${it.antall}, "${it.tabell}"]"""
                }.joinToString()
            )
            .replace(
                "@GravidKravTiltak", //language=JSON
                """{"value": ${gravidSoeknadTiltak.hjemmekontor}, "name": "Hjemmekontor"},
                   {"value": ${gravidSoeknadTiltak.tipasset_arbeidstid}, "name": "Tilpasset Arbeidstid"},
                   {"value": ${gravidSoeknadTiltak.tilpassede_arbeidsoppgaver}, "name": "Tilpassede Arbeidsoppgaver"},
                   {"value": ${gravidSoeknadTiltak.annet}, "name": "Annet"}
                """.trimIndent()
            )

            /*.replace(
                "@sykegrad_uker",
                sykegrad.map { it.uke }.distinct().joinToString()
            )
            .replace(
                "@bucket1",
                sykegrad.filter { it.bucket == 1 }.map { it.antall }.joinToString()
            )
            .replace(
                "@bucket2",
                sykegrad.filter { it.bucket == 2 }.map { it.antall }.joinToString()
            )
            .replace(
                "@bucket3",
                sykegrad.filter { it.bucket == 3 }.map { it.antall }.joinToString()
            )
            .replace(
                "@bucket4",
                sykegrad.filter { it.bucket == 4 }.map { it.antall }.joinToString()
            )
            .replace(
                "@bucket5",
                sykegrad.filter { it.bucket == 5 }.map { it.antall }.joinToString()
            )*/

        runBlocking {
            logger.info("Populerte datapakke template med data: $populatedDatapakke")

            val response = httpClient.put<HttpResponse>("$datapakkeApiUrl/$datapakkeId") {
                contentType(ContentType.Application.Json)
                body = om.readTree(populatedDatapakke)
            }

            logger.info("Oppdaterte datapakke $datapakkeId med respons ${response.readText()}")
        }
    }
}
