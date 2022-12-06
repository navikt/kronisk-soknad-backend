package no.nav.helse.fritakagp.koin

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.config.ApplicationConfig
import io.ktor.http.ContentType
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.fritakagp.customObjectMapper
import org.koin.core.module.Module
import org.koin.dsl.module

fun selectModuleBasedOnProfile(config: ApplicationConfig): List<Module> {
    val envModule = when (config.property("koin.profile").getString()) {
        "LOCAL" -> localDevConfig(config)
        "PREPROD" -> preprodConfig(config)
        "PROD" -> prodConfig(config)
        else -> localDevConfig(config)
    }
    return listOf(common, envModule)
}

val common = module {
    single { customObjectMapper() }

    single { KubernetesProbeManager() }

    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            val objectMapper = customObjectMapper(false)
            serializer = JacksonSerializer(objectMapper) {
                accept(ContentType.Application.Json)
            }
        }
    }

    single { httpClient }
}

// utils

fun ApplicationConfig.getjdbcUrlFromProperties(): String {
    return String.format(
        "jdbc:postgresql://%s:%s/%s",
        this.property("database.host").getString(),
        this.property("database.port").getString(),
        this.property("database.name").getString()
    )
}
