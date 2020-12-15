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
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostRequest
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveResponse
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.*
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.fritakagp.processing.kvittering.DummyKvitteringSender
import no.nav.helse.fritakagp.processing.kvittering.KvitteringSender
import org.koin.core.Koin
import org.koin.core.definition.Kind
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module


@KtorExperimentalAPI
fun selectModuleBasedOnProfile(config: ApplicationConfig): List<Module> {
    val envModule = when (config.property("koin.profile").getString()) {
        "TEST" -> buildAndTestConfig()
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

fun buildAndTestConfig() = module {
    single { DummyKvitteringSender() as KvitteringSender }
}

fun Module.mockExternalDependecies() {
    single { object: DokarkivKlient {
        override fun journalførDokument(
            journalpost: JournalpostRequest,
            forsoekFerdigstill: Boolean,
            callId: String
        ): JournalpostResponse {
            return JournalpostResponse("arkiv-ref", true, "J", null, emptyList())
        }
    } } bind DokarkivKlient::class

    single {object: PdlClient {
        override fun fullPerson(ident: String) =
            PdlHentFullPerson(
                PdlHentFullPerson.PdlFullPersonliste(
                    emptyList(),
                    emptyList(),
                    PdlHentFullPerson.PdlFullPersonliste.PdlGeografiskTilknytning(
                        PdlHentFullPerson.PdlFullPersonliste.PdlGeografiskTilknytning.PdlGtType.UTLAND,
                        null,
                        null,
                        "SWE"
                    ),
                    emptyList(),
                    emptyList(),
                    emptyList()
                ),
                PdlHentFullPerson.PdlIdentResponse(listOf(PdlIdent("aktør-id", PdlIdent.PdlIdentGruppe.AKTORID)))
            )

        override fun personNavn(ident: String) =
            PdlHentPersonNavn.PdlPersonNavneliste(listOf(
                PdlHentPersonNavn.PdlPersonNavneliste.PdlPersonNavn("Ola", "M", "Avsender", PdlPersonNavnMetadata("freg"))))
    }

    } bind PdlClient::class

    single { object: OppgaveKlient {
        override suspend fun opprettOppgave(
            opprettOppgaveRequest: OpprettOppgaveRequest,
            callId: String
        ): OpprettOppgaveResponse = OpprettOppgaveResponse(1234)
    } } bind OppgaveKlient::class

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
