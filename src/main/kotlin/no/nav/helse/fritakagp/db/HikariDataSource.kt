package no.nav.helse.fritakagp.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

fun createHikariDataSource(jdbcUrl: String, username: String, password: String): HikariDataSource =
    HikariConfig()
        .apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 2000
            maxLifetime = 30001
            driverClassName = "org.postgresql.Driver"
            poolName = "defaultPool"
        }
        .let(::HikariDataSource)
