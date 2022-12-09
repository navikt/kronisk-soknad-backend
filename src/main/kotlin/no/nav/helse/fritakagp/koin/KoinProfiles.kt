package no.nav.helse.fritakagp.koin

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.config.ApplicationConfig
import io.ktor.http.ContentType
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.fritakagp.AppEnv
import no.nav.helse.fritakagp.Env
import no.nav.helse.fritakagp.customObjectMapper
import org.koin.core.module.Module
import org.koin.dsl.module

fun selectModuleBasedOnProfile(env: Env, config: ApplicationConfig): List<Module> {
    val envModule = when (env.appEnv) {
        AppEnv.PROD -> ::prodConfig
        AppEnv.PREPROD -> ::preprodConfig
        AppEnv.LOCAL -> ::localConfig
    }
        .invoke(config)

    return listOf(common, envModule)
}

private val common = module {
    single { customObjectMapper() }

    single {
        HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    registerModule(KotlinModule())
                    registerModule(Jdk8Module())
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    configure(SerializationFeature.INDENT_OUTPUT, true)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                    accept(ContentType.Application.Json)
                }
            }
        }
    }

    single { KubernetesProbeManager() }
}
