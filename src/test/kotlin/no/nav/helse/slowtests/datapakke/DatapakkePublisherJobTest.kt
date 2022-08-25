package no.nav.helse.slowtests.datapakke

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.helse.fritakagp.datapakke.DatapakkePublisherJob
import no.nav.helse.fritakagp.db.GravidSoeknadTiltak
import no.nav.helse.fritakagp.db.IStatsRepo
import no.nav.helse.fritakagp.db.SykeGradAntall
// import no.nav.helse.fritakagp.db.SykeGradAntall
import no.nav.helse.fritakagp.db.WeeklyStats
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import no.nav.security.mock.oauth2.http.objectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.random.Random

class DatapakkePublisherJobTest : SystemTestBase() {
    val repo = mockk<IStatsRepo>()

    @BeforeAll
    internal fun setUp() {
        val weeks = (7..52) + (1..6)
        val gravidSoeknad = weeks.map { WeeklyStats(it, Random.nextInt(30), "gravid_soeknad") }
        val kroniskSoeknad = weeks.map { WeeklyStats(it, Random.nextInt(30), "kronisk_soeknad") }
        val gravidKrav = weeks.map { WeeklyStats(it, Random.nextInt(30), "gravid_krav") }
        val kroniskKrav = weeks.map { WeeklyStats(it, Random.nextInt(30), "kronisk_krav") }
        val weeklyStats = gravidSoeknad + kroniskSoeknad + gravidKrav + kroniskKrav

        every { repo.getWeeklyStats() } returns weeklyStats.toList()

        every { repo.getGravidSoeknadTiltak() } returns GravidSoeknadTiltak(Random.nextInt(30), Random.nextInt(30), Random.nextInt(30), Random.nextInt(30))

        val list = ArrayList<SykeGradAntall>()
        weeks.map { uke ->
            (1..6).map { bucket ->
                if (bucket > 1) {
                    list.add(SykeGradAntall(Random.nextInt(100), bucket, uke))
                }
                if (bucket == 1 && uke < 52) {
                    list.add(SykeGradAntall(Random.nextInt(100), bucket, uke))
                }
            }
        }

        every { repo.getSykeGradAntall() } returns list
    }

    @Test
    @Disabled
    internal fun name() = suspendableTest {
        DatapakkePublisherJob(
            repo,
            httpClient,
            "https://datakatalog-api.dev.intern.nav.no/v1/datapackage",
            "a4570c9e9521df726e1050ebcd917b9a",
            om = objectMapper
        ).doJob()
    }

    @Test
    @Disabled
    fun `print data`() {
        val testData = "datapakke/datapakke-fritak.json".loadFromResources()
        val timeseries = repo.getWeeklyStats()
        val gravidSoeknadTiltak = repo.getGravidSoeknadTiltak()
        val kroniskArbeidstyper = repo.getKroniskSoeknadArbeidstyper()
        val kroniskPaakjenningstyper = repo.getKroniskSoeknadPaakjenningstyper()
        val populatedDatapakke = testData
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
            .replace(
                "@KroniskArbeidstyper",
                kroniskArbeidstyper.map { //language=JSON
                    """{"value": ${it.antall}, "name": "${it.type.trim()}"}"""
                }.joinToString()
            )
            .replace(
                "@KroniskPaakjenningstyper",
                kroniskPaakjenningstyper.map { //language=JSON
                    """{"value": ${it.antall}, "name": "${it.type.trim()}"}"""
                }.joinToString()
            )

        val result = objectMapper.readTree(populatedDatapakke)
        println(result)
    }
}
