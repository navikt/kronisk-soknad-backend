package no.nav.helse.fritakagp.web

import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.arbeidsgiver.system.AppEnv
import no.nav.helse.arbeidsgiver.system.getEnvironment
import no.nav.helse.fritakagp.koin.getAllOfType
import no.nav.helse.fritakagp.koin.selectModuleBasedOnProfile
import no.nav.helse.fritakagp.processing.gravid.SoeknadGravidProcessor
import no.nav.helse.fritakagp.web.auth.localCookieDispenser
import org.flywaydb.core.Flyway
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.slf4j.LoggerFactory


val mainLogger = LoggerFactory.getLogger("main")

@KtorExperimentalAPI
fun main() {

    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        mainLogger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    embeddedServer(Netty, createApplicationEnvironment()).let { engine ->

        val environment = engine.environment.config.getEnvironment()
        if(environment == AppEnv.PREPROD || environment == AppEnv.PROD) {
            mainLogger.info("Sover i 30s i påvente av SQL proxy sidecar")
            Thread.sleep(30000)
        }

        startKoin { modules(selectModuleBasedOnProfile(engine.environment.config)) }
        val koin = GlobalContext.get().koin

        migrateDatabase()

        startBackgroundWorker(koin)

        engine.start(wait = false)
        runBlocking { autoDetectProbeableComponents(koin) }
        mainLogger.info("La til probeable komponenter")

        Runtime.getRuntime().addShutdownHook(Thread {
            engine.stop(1000, 1000)
        })
    }
}

private fun startBackgroundWorker(koin: Koin) {
    val bakgrunnsjobbService = koin.get<BakgrunnsjobbService>()
    bakgrunnsjobbService.leggTilBakgrunnsjobbProsesserer(
        SoeknadGravidProcessor.JOB_TYPE,
        koin.get<SoeknadGravidProcessor>()
    )
    bakgrunnsjobbService.startAsync(true)
}

private fun migrateDatabase() {
    mainLogger.info("Starter databasemigrering")

    Flyway.configure()
            .dataSource(GlobalContext.get().koin.get())
            .load()
            .migrate()

    mainLogger.info("Databasemigrering slutt")
}

private suspend fun autoDetectProbeableComponents(koin: org.koin.core.Koin) {
    val kubernetesProbeManager = koin.get<KubernetesProbeManager>()

    koin.getAllOfType<LivenessComponent>()
            .forEach { kubernetesProbeManager.registerLivenessComponent(it) }

    koin.getAllOfType<ReadynessComponent>()
            .forEach { kubernetesProbeManager.registerReadynessComponent(it) }
}


@KtorExperimentalAPI
fun createApplicationEnvironment() = applicationEngineEnvironment {
    config = HoconApplicationConfig(ConfigFactory.load())

    connector {
        port = 8080
    }

    module {

        if (config.getEnvironment() != AppEnv.PROD) {
            localCookieDispenser(config)
        }

        fritakModule(config)
    }
}

