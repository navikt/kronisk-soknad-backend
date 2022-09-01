package no.nav.helse.fritakagp.koin

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.config.ApplicationConfig
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.fritakagp.config.AppEnv
import no.nav.helse.fritakagp.config.env
import org.koin.core.module.Module
import org.koin.dsl.module

fun selectModuleBasedOnProfile(config: ApplicationConfig): List<Module> {
    val envModule = when (config.env()) {
        AppEnv.PROD -> ::prodConfig
        AppEnv.PREPROD -> ::preprodConfig
        AppEnv.LOCAL -> ::localDevConfig
    }
        .invoke(config)

    return listOf(common, envModule)
}

private val common = module {
    single {
        ObjectMapper().apply {
            registerKotlinModule()
            configureCustom()

            setDefaultPrettyPrinter(
                DefaultPrettyPrinter().apply {
                    indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                    indentObjectsWith(DefaultIndenter("  ", "\n"))
                }
            )
        }
    }

    single {
        HttpClient(Apache) {
            install(ContentNegotiation) {
                jackson {
                    configureCustom()
                }
            }
        }
    }

    single { KubernetesProbeManager() }
}

private fun ObjectMapper.configureCustom() {
    registerModules(
        Jdk8Module(),
        JavaTimeModule(),
    )
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    configure(SerializationFeature.INDENT_OUTPUT, true)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
}
