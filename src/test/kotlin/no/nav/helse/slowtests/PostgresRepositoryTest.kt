package no.nav.helse.slowtests

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.fritakagp.db.PostgresRepository
import no.nav.helse.fritakagp.db.createHikariConfig
import no.nav.helse.fritakagp.web.common
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get

class PostgresRepositoryTest : KoinComponent {

//TODO: få opp lokal database til testing og lokal kjøring, og så skrive tester

}
