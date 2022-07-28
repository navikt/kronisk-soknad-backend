package no.nav.helse.slowtests

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.fritakagp.db.SimpleJsonbEntity
import no.nav.helse.fritakagp.db.SimpleJsonbRepository
import no.nav.helse.fritakagp.db.createHikariDataSource
import org.assertj.core.api.AbstractIntegerAssert
import org.assertj.core.api.Assertions
import org.assertj.core.api.ListAssert
import org.assertj.core.api.ObjectAssert

fun createTestHikariDataSource(): HikariDataSource =
    createHikariDataSource(
        jdbcUrl = "jdbc:postgresql://localhost:5432/fritakagp_db",
        username = "fritakagp",
        password = "fritakagp",
    )

fun <T : SimpleJsonbEntity> SimpleJsonbRepository<T>.getAll(): List<T> =
    getAll("SELECT * FROM $tableName")

fun Int.assert(assertion: (AbstractIntegerAssert<*>) -> Unit): Int {
    this.let(Assertions::assertThat).let { assertion(it) }
    return this
}

fun <T : SimpleJsonbEntity> T?.assert(assertion: (ObjectAssert<T?>) -> Unit): T? {
    this.let(Assertions::assertThat).let { assertion(it) }
    return this
}

fun <T : Any> List<T>.assert(assertion: (ListAssert<T>) -> Unit): List<T> {
    this.let(Assertions::assertThat).let { assertion(it) }
    return this
}
