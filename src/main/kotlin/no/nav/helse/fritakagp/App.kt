package no.nav.helse.fritakagp

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.fritakagp.koin.profileModules
import no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonProcessor
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKafkaProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravKvitteringProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.GravidKravSlettProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.OpprettRobotOppgaveGravidProcessor
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKafkaProcessor
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadKvitteringProcessor
import no.nav.helse.fritakagp.processing.gravid.soeknad.GravidSoeknadProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKafkaProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravKvitteringProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.KroniskKravSlettProcessor
import no.nav.helse.fritakagp.processing.kronisk.krav.OpprettRobotOppgaveKroniskProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKafkaProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadKvitteringProcessor
import no.nav.helse.fritakagp.processing.kronisk.soeknad.KroniskSoeknadProcessor
import no.nav.helse.fritakagp.web.auth.localAuthTokenDispenser
import no.nav.helse.fritakagp.web.fritakModule
import no.nav.helse.fritakagp.web.nais.nais
import org.flywaydb.core.Flyway
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.slf4j.LoggerFactory

class FritakAgpApplication(val port: Int = 8080) : KoinComponent {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val appConfig = HoconApplicationConfig(ConfigFactory.load())
    private val env = readEnv(appConfig)

    private val webserver: NettyApplicationEngine

    init {
        if (env is Env.Preprod || env is Env.Prod) {
            logger.info("Sover i 30s i påvente av SQL proxy sidecar")
            Thread.sleep(30000)
        }

        startKoin { modules(profileModules(env)) }
        migrateDatabase()

        configAndStartBackgroundWorker()
        autoDetectProbeableComponents()

        webserver = createWebserver().also {
            it.start(wait = false)
        }
    }

    fun shutdown() {
        webserver.stop(1000, 1000)
        get<BakgrunnsjobbService>().stop()
        stopKoin()
    }

    private fun createWebserver(): NettyApplicationEngine =
        embeddedServer(
            Netty,
            applicationEngineEnvironment {
                config = appConfig
                connector {
                    port = this@FritakAgpApplication.port
                }

                module {
                    localAuthTokenDispenser(env)
                    nais()
                    fritakModule(env)
                }
            }
        )

    private fun configAndStartBackgroundWorker() {
        get<BakgrunnsjobbService>().apply {
            registrer(get<GravidSoeknadProcessor>())
            registrer(get<GravidSoeknadKafkaProcessor>())
            registrer(get<GravidSoeknadKvitteringProcessor>())

            registrer(get<GravidKravProcessor>())
            registrer(get<GravidKravKafkaProcessor>())
            registrer(get<GravidKravKvitteringProcessor>())
            registrer(get<GravidKravSlettProcessor>())
            registrer(get<OpprettRobotOppgaveGravidProcessor>())

            registrer(get<KroniskSoeknadProcessor>())
            registrer(get<KroniskSoeknadKafkaProcessor>())
            registrer(get<KroniskSoeknadKvitteringProcessor>())

            registrer(get<KroniskKravProcessor>())
            registrer(get<KroniskKravKafkaProcessor>())
            registrer(get<KroniskKravKvitteringProcessor>())
            registrer(get<KroniskKravSlettProcessor>())
            registrer(get<OpprettRobotOppgaveKroniskProcessor>())

            registrer(get<BrukernotifikasjonProcessor>())
            registrer(get<ArbeidsgiverNotifikasjonProcessor>())

            startAsync(true)
        }
    }

    private fun migrateDatabase() {
        logger.info("Starter databasemigrering")

        Flyway.configure().baselineOnMigrate(true)
            .dataSource(GlobalContext.getKoinApplicationOrNull()?.koin?.get())
            .load()
            .migrate()

        logger.info("Databasemigrering slutt")
    }

    private fun autoDetectProbeableComponents() {
        val kubernetesProbeManager = get<KubernetesProbeManager>()

        getKoin().getAll<LivenessComponent>()
            .forEach { kubernetesProbeManager.registerLivenessComponent(it) }

        getKoin().getAll<ReadynessComponent>()
            .forEach { kubernetesProbeManager.registerReadynessComponent(it) }

        logger.debug("La til probeable komponenter")
    }
}

fun main() {
    val logger = LoggerFactory.getLogger("fritakagp")
    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        logger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    val application = FritakAgpApplication()

    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info("Fikk shutdown-signal, avslutter...")
            application.shutdown()
            logger.info("Avsluttet OK")
        }
    )
}
