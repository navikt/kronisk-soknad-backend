package no.nav.helse.arbeidsgiver.bakgrunnsjobb2

import io.ktor.client.plugins.ResponseException
import io.ktor.util.InternalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.utils.RecurringJob
import java.time.LocalDateTime
import kotlin.collections.HashMap

class ParallellBakgrunnsjobbService(
    val bakgrunnsjobbRepository: BakgrunnsjobbRepository,
    val delayMillis: Long = 30 * 1000L,
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    val bakgrunnsvarsler: Bakgrunnsvarsler = TomVarsler()
) : RecurringJob(coroutineScope, delayMillis) {

    val prossesserere = HashMap<String, BakgrunnsjobbProsesserer>()

    fun registrer(prosesserer: BakgrunnsjobbProsesserer) {
        prossesserere[prosesserer.type] = prosesserer
    }

    override fun doJob() {
        finnVentende()
            .also { logger.debug("Fant ${it.size} bakgrunnsjobber å kjøre") }
            .onEach {
                coroutineScope.launch {
                    prosesser(it)
                }
            }
    }

    fun prosesser(jobb: Bakgrunnsjobb) {
        jobb.behandlet = LocalDateTime.now()
        jobb.forsoek++

        val prossessorForType = prossesserere[jobb.type]
            ?: throw IllegalArgumentException("Det finnes ingen prossessor for typen '${jobb.type}'. Dette må konfigureres.")

        try {
            jobb.kjoeretid = prossessorForType.nesteForsoek(jobb.forsoek, LocalDateTime.now())
            prossessorForType.prosesser(jobb.copy())
            jobb.status = BakgrunnsjobbStatus.OK
            OK_JOBB_COUNTER.labels(jobb.type).inc()
        } catch (ex: Throwable) {
            val responseBody = tryGetResponseBody(ex)
            val responseBodyMessage = if (responseBody != null) "Feil fra ekstern tjeneste: $responseBody" else ""
            jobb.status = if (jobb.forsoek >= jobb.maksAntallForsoek) BakgrunnsjobbStatus.STOPPET else BakgrunnsjobbStatus.FEILET
            if (jobb.status == BakgrunnsjobbStatus.STOPPET) {
                logger.error("Jobb ${jobb.uuid} feilet permanent og ble stoppet fra å kjøre igjen. $responseBodyMessage", ex)
                STOPPET_JOBB_COUNTER.labels(jobb.type).inc()
                bakgrunnsvarsler.rapporterPermanentFeiletJobb()
                tryStopAction(prossessorForType, jobb)
            } else {
                logger.error("Jobb ${jobb.uuid} feilet, forsøker igjen ${jobb.kjoeretid}. $responseBodyMessage", ex)
                FEILET_JOBB_COUNTER.labels(jobb.type).inc()
            }
        } finally {
            bakgrunnsjobbRepository.update(jobb)
        }
    }

    fun finnVentende(): List<Bakgrunnsjobb> =
        bakgrunnsjobbRepository.findByKjoeretidBeforeAndStatusIn(
            LocalDateTime.now(),
            setOf(BakgrunnsjobbStatus.OPPRETTET, BakgrunnsjobbStatus.FEILET)
        )

    private fun tryStopAction(prossessorForType: BakgrunnsjobbProsesserer, jobb: Bakgrunnsjobb) {
        try {
            prossessorForType.stoppet(jobb)
            logger.error("Jobben ${jobb.uuid} kjørte sin opprydningsjobb!")
        } catch (ex: Throwable) {
            logger.error("Jobben ${jobb.uuid} feilet i sin opprydningsjobb!", ex)
        }
    }

    @OptIn(InternalAPI::class) // TODO: Fix
    private fun tryGetResponseBody(jobException: Throwable): String? {
        if (jobException is ResponseException) {
            return try {
                runBlocking { jobException.response.content.readUTF8Line(1_000_000) }
            } catch (readEx: Exception) {
                null
            }
        }
        return null
    }
}
