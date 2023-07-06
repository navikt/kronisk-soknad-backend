package no.nav.helse.fritakagp.koin

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.fritakagp.Env
import no.nav.helse.fritakagp.configureCustom
import no.nav.helse.fritakagp.customObjectMapper
import org.koin.core.module.Module
import org.koin.dsl.module

fun profileModules(env: Env): List<Module> {
    val envModule = when (env) {
        is Env.Prod -> prodConfig(env)
        is Env.Preprod -> preprodConfig(env)
        is Env.Local -> localConfig(env)
    }

    return listOf(common, envModule)
}

private val common = module {
    single { customObjectMapper() }

    single {
        HttpClient(Apache) {
            expectSuccess = true
            install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    configureCustom()
                }
            }
        }
    }

    single { KubernetesProbeManager() }
}
