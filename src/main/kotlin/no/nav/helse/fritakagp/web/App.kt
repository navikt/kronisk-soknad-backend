package no.nav.helse.fritakagp.web

import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.arbeidsgiver.system.AppEnv
import no.nav.helse.arbeidsgiver.system.getEnvironment
import no.nav.helse.fritakagp.koin.getAllOfType
import no.nav.helse.fritakagp.koin.selectModuleBasedOnProfile
import no.nav.helse.fritakagp.nais.nais
import no.nav.helse.fritakagp.processing.gravid.SoeknadGravidProcessor
import no.nav.helse.fritakagp.web.auth.localCookieDispenser
import org.flywaydb.core.Flyway
import org.koin.core.KoinComponent
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get
import org.slf4j.LoggerFactory

class FritakAgpApplication(val port: Int = 8080) : KoinComponent {
    private val logger = LoggerFactory.getLogger(FritakAgpApplication::class.simpleName)
    private var webserver: NettyApplicationEngine? = null
    private var appConfig: HoconApplicationConfig = HoconApplicationConfig(ConfigFactory.load())
    private val runtimeEnvironment = appConfig.getEnvironment()

    @KtorExperimentalAPI
    fun start() {
        if (runtimeEnvironment == AppEnv.PREPROD || runtimeEnvironment == AppEnv.PROD) {
            logger.info("Sover i 30s i påvente av SQL proxy sidecar")
            Thread.sleep(30000)
        }

        startKoin { modules(selectModuleBasedOnProfile(appConfig)) }
        migrateDatabase()

        configAndStartWebserver()
        configAndStartBackgroundWorker()
        autoDetectProbeableComponents()
    }

    fun shutdown() {
        webserver?.stop(1000, 1000)
        get<BakgrunnsjobbService>().stop()
        stopKoin()
    }

    private fun configAndStartWebserver() {
        webserver = embeddedServer(Netty, applicationEngineEnvironment {
            config = appConfig

            connector {
                port = this@FritakAgpApplication.port
            }

            module {
                if (runtimeEnvironment != AppEnv.PROD) {
                    localCookieDispenser(config)
                }

                nais()
                fritakModule(config)
            }
        })

        webserver!!.start(wait = false)
    }

    private fun configAndStartBackgroundWorker() {
        val bakgrunnsjobbService = get<BakgrunnsjobbService>()

        bakgrunnsjobbService.leggTilBakgrunnsjobbProsesserer(
            SoeknadGravidProcessor.JOB_TYPE,
            get<SoeknadGravidProcessor>()
        )

        bakgrunnsjobbService.startAsync(true)
    }

    private fun migrateDatabase() {
        logger.info("Starter databasemigrering")

        Flyway.configure()
            .dataSource(GlobalContext.get().koin.get())
            .baselineOnMigrate(true)
            .load()
            .migrate()

        logger.info("Databasemigrering slutt")
    }

    private fun autoDetectProbeableComponents() {
        val kubernetesProbeManager = get<KubernetesProbeManager>()

        getKoin().getAllOfType<LivenessComponent>()
            .forEach { kubernetesProbeManager.registerLivenessComponent(it) }

        getKoin().getAllOfType<ReadynessComponent>()
            .forEach { kubernetesProbeManager.registerReadynessComponent(it) }

        logger.debug("La til probeable komponenter")
    }
}


@KtorExperimentalAPI
fun main() {
    val logger = LoggerFactory.getLogger("main")

    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        logger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    val application = FritakAgpApplication()
    application.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Fikk shutdown-signal, avslutter...")
        application.shutdown()
        logger.info("Avsluttet OK")
    })
}
