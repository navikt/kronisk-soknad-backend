package no.nav.helse.fritakagp.db

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helsearbeidsgiver.utils.logger
import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

interface SimpleJsonbEntity {
    val id: UUID
    val opprettet: LocalDateTime
    val virksomhetsnummer: String
}

sealed interface SimpleJsonbRepository<T : SimpleJsonbEntity> {
    val tableName: String

    fun getById(id: UUID): T?
    fun getAllByVirksomhet(virksomhetsnummer: String): List<T>
    fun getAll(sql: String): List<T>

    fun insert(entity: T): Int
    fun insert(entity: T, connection: Connection): Int

    fun delete(id: UUID): Int
    fun delete(id: UUID, connection: Connection): Int
    fun deleteAllOpprettetFoer(opprettetFoer: LocalDateTime): Int

    fun update(entity: T): Int
    fun update(entity: T, connection: Connection): Int
}

/**
 * Enkelt CRUD-base for enkle UUID-id'ede typer.
 * Tabellen må ha kun en kolonne av typen jsonb, og entitietene må ha ett felt av typen UUID som heter id.
 * SQL:
 *  CREATE TABLE $tableName (data jsonb not null);
 */
sealed class SimpleJsonbRepositoryBase<T : SimpleJsonbEntity>(
    override val tableName: String,
    private val ds: DataSource,
    private val mapper: ObjectMapper,
    private val clazz: Class<T>
) : SimpleJsonbRepository<T> {
    private val logger = this.logger()

    override fun getById(id: UUID): T? =
        "SELECT * FROM $tableName WHERE data ->> 'id' = '$id'"
            .executeQuery(
                "SELECT med ID mot $tableName feilet. Gjelder ID=$id."
            )
            .firstOrNull()

    override fun getAllByVirksomhet(virksomhetsnummer: String): List<T> =
        "SELECT * FROM $tableName WHERE data ->> 'virksomhetsnummer' = '$virksomhetsnummer'"
            .executeQuery(null)

    override fun getAll(sql: String): List<T> =
        sql.executeQuery(null)

    override fun insert(entity: T, connection: Connection): Int =
        "INSERT INTO $tableName (data) VALUES ('${entity.toJson()}'::json)"
            .executeUpdate(
                connection,
                "INSERT mot $tableName feilet. Mulig duplikat. Gjelder objekt med ID=${entity.id}.",
            )

    override fun insert(entity: T): Int =
        ds.connection.use {
            insert(entity, it)
        }

    override fun delete(id: UUID, connection: Connection): Int =
        "DELETE FROM $tableName WHERE data ->> 'id' = '$id'"
            .executeUpdate(
                connection,
                "DELETE mot $tableName feilet. Gjelder ID=$id.",
            )

    override fun delete(id: UUID): Int =
        ds.connection.use {
            delete(id, it)
        }

    override fun deleteAllOpprettetFoer(opprettetFoer: LocalDateTime): Int =
        ds.connection.use {
            "DELETE FROM $tableName WHERE data ->> 'opprettet' < '$opprettetFoer'"
                .executeUpdate(it, null)
        }

    override fun update(entity: T, connection: Connection): Int =
        "UPDATE $tableName SET data = '${entity.toJson()}'::json WHERE data ->> 'id' = '${entity.id}'"
            .executeUpdate(
                connection,
                "UPDATE mot $tableName feilet. Gjelder objekt med ID=${entity.id}.",
            )

    override fun update(entity: T): Int =
        ds.connection.use {
            update(entity, it)
        }

    private fun String.executeQuery(failureMsg: String?): List<T> =
        ds.connection.use {
            val rs = it.prepareStatement(this)
                .executeQuery()

            val results = mutableListOf<T>()
            while (rs.next()) {
                rs.toObject()
                    .let(results::add)
            }
            results
        }
            .also {
                if (it.isEmpty() && failureMsg != null)
                    logger.info(failureMsg)
            }

    private fun String.executeUpdate(connection: Connection, failureMsg: String?): Int =
        connection.prepareStatement(this)
            .executeUpdate()
            .also {
                if (it == 0 && failureMsg != null)
                    logger.info(failureMsg)
            }

    private fun ResultSet.toObject(): T =
        this.getString("data")!!
            .let(::fromJson)

    private fun T.toJson(): String =
        mapper.writeValueAsString(this)

    private fun fromJson(json: String): T =
        mapper.readValue(json, clazz)
}
