package no.nav.helse.fritakagp.koin

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.fritakagp.db.MockSoeknadRepo
import no.nav.helse.fritakagp.db.PostgresRepository
import no.nav.helse.fritakagp.db.Repository
import no.nav.helse.fritakagp.db.createHikariConfig
import no.nav.helse.fritakagp.intergrasjoner.virusscan.ClamavVirusScannerImp
import no.nav.helse.fritakagp.intergrasjoner.virusscan.MockVirusScanner
import no.nav.helse.fritakagp.intergrasjoner.virusscan.VirusScanner
import no.nav.helse.fritakagp.web.auth.LocalOIDCWireMock
import org.koin.core.Koin
import org.koin.core.definition.Kind
import org.koin.core.module.Module
import org.koin.dsl.module
import javax.sql.DataSource


@KtorExperimentalAPI
fun selectModuleBasedOnProfile(config: ApplicationConfig): List<Module> {
    val envModule = when (config.property("koin.profile").getString()) {
        "TEST" -> buildAndTestConfig(config)
        "LOCAL" -> localDevConfig(config)
        "PREPROD" -> preprodConfig(config)
        "PROD" -> prodConfig(config)
        else -> localDevConfig(config)
    }
    return listOf(common, envModule)
}

val common = module {
    val om = ObjectMapper()
    om.registerModule(KotlinModule())
    om.registerModule(Jdk8Module())
    om.registerModule(JavaTimeModule())
    om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    om.configure(SerializationFeature.INDENT_OUTPUT, true)
    om.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
    om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    om.setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
        indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
        indentObjectsWith(DefaultIndenter("  ", "\n"))
    })

    single { om }

    single { KubernetesProbeManager() }

    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerModule(KotlinModule())
                registerModule(Jdk8Module())
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                configure(SerializationFeature.INDENT_OUTPUT, true)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            }
        }
    }

    single { httpClient }

}

fun buildAndTestConfig(config: ApplicationConfig) = module {
    single { HikariDataSource(createHikariConfig(config.getjdbcUrlFromProperties(), config.getString("database.username"), config.getString("database.password"))) as DataSource }
    single { MockSoeknadRepo() as Repository}
    single { MockVirusScanner() as VirusScanner }
    LocalOIDCWireMock.start()
}

@KtorExperimentalAPI
fun localDevConfig(config: ApplicationConfig) = module {
    single { HikariDataSource(createHikariConfig(config.getjdbcUrlFromProperties(), config.getString("database.username"), config.getString("database.password"))) as DataSource }
    single { PostgresRepository(get(), get()) as Repository}
    //single { MockVirusScanner() as VirusScanner }
    single { ClamavVirusScannerImp(
        get(),
        config.getString("clam_local_url")
    ) as VirusScanner }

    LocalOIDCWireMock.start()
}

@KtorExperimentalAPI
fun preprodConfig(config: ApplicationConfig) = module {
    single { HikariDataSource(createHikariConfig(config.getjdbcUrlFromProperties(), config.getString("database.username"), config.getString("database.password"))) as DataSource }
    single { PostgresRepository(get(), get()) as Repository}
    single { ClamavVirusScannerImp(
            get(),
            config.getString("clam_on_prem_url")
    ) as VirusScanner }
}

@KtorExperimentalAPI
fun prodConfig(config: ApplicationConfig) = module {
    single { ClamavVirusScannerImp(
            get(),
            config.getString("clam_gcp_url")
    ) as VirusScanner }
}

// utils
@KtorExperimentalAPI
fun ApplicationConfig.getString(path: String): String {
    return this.property(path).getString()
}

@KtorExperimentalAPI
fun ApplicationConfig.getjdbcUrlFromProperties(): String {
    return String.format("jdbc:postgresql://%s:%s/%s",
            this.property("database.host").getString(),
            this.property("database.port").getString(),
            this.property("database.name").getString())
}


inline fun <reified T : Any> Koin.getAllOfType(): Collection<T> =
        let { koin ->
            koin.rootScope.beanRegistry
                    .getAllDefinitions()
                    .filter { it.kind == Kind.Single }
                    .map { koin.get<Any>(clazz = it.primaryType, qualifier = null, parameters = null) }
                    .filterIsInstance<T>()
        }
