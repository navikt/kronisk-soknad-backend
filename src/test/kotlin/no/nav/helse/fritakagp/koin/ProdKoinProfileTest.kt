package no.nav.helse.fritakagp.koin

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.fritakagp.Env
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonService
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import javax.sql.DataSource

@ExtendWith(MockKExtension::class)
class ProdKoinProfileTest : KoinTest {

    private val env: Env.Prod = mockk()
    private val dataSource: DataSource by inject()
    private val gravidSoeknadRepository: GravidSoeknadRepository by inject()
    private val kroniskSoeknadRepository: KroniskSoeknadRepository by inject()
    private val gravidKravRepository: GravidKravRepository by inject()
    private val kroniskKravRepository: KroniskKravRepository by inject()
    private val bakgrunnsjobbRepository: BakgrunnsjobbRepository by inject()
    private val bakgrunnsjobbService: BakgrunnsjobbService by inject()
    private val brukernotifikasjonService: BrukernotifikasjonService by inject()
    private val objectMapper: ObjectMapper by inject()

    @Test
    fun `test prodConfig`() {
        every { env.databaseUrl } returns "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
        every { env.databaseUsername } returns "sa"
        every { env.databasePassword } returns ""
        every { env.frontendUrl } returns "https://frontend.url"
        every { env.altinnMeldingUrl } returns "https://altinn.url"
        every { env.altinnMeldingServiceId } returns "serviceId"
        every { env.altinnMeldingUsername } returns "username"
        every { env.altinnMeldingPassword } returns "password"
        every { env.brregUrl } returns "https://brreg.url"
        every { env.oauth2 } returns mockk()

        val testModule = module {
            single {
                HikariConfig().apply {
                    jdbcUrl = env.databaseUrl
                    username = env.databaseUsername
                    password = env.databasePassword
                    driverClassName = "org.h2.Driver"
                }
            }
            single<DataSource> { HikariDataSource(get()) }
        }

        startKoin {
            modules(profileModules(env)[0], prodConfig(env), testModule)
        }

        assertNotNull(dataSource)
        assertNotNull(gravidSoeknadRepository)
        assertNotNull(kroniskSoeknadRepository)
        assertNotNull(gravidKravRepository)
        assertNotNull(kroniskKravRepository)
        assertNotNull(bakgrunnsjobbRepository)
        assertNotNull(bakgrunnsjobbService)
        assertNotNull(brukernotifikasjonService)
        assertNotNull(objectMapper)

        stopKoin()
    }
}
