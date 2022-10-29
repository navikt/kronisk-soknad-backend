package no.nav.helse.fritakagp.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

interface SimpleJsonbEntity {
    val id: UUID
}

interface SimpleJsonbRepository<T : SimpleJsonbEntity> {
    fun getById(id: UUID): T?

    fun insert(soeknad: T): T
    fun insert(soeknad: T, connection: Connection): T

    fun delete(id: UUID): Int
    fun delete(id: UUID, connection: Connection): Int

    fun update(soeknad: T)
    fun update(soeknad: T, connection: Connection)
}

/**
 * Enkelt CRUD-base for enkle UUID-id'ede typer.
 * Tabellen må ha kun en kolonne av typen jsonb, og entitietene må ha ett felt av typen UUID som heter id.
 * SQL:
 *  CREATE TABLE $tableName (data jsonb not null);
 */
abstract class SimpleJsonbRepositoryBase<T : SimpleJsonbEntity>(
    val tableName: String,
    val ds: DataSource,
    val mapper: ObjectMapper,
    val clazz: Class<T>
) : SimpleJsonbRepository<T> {

    private val getByIdStatement = """SELECT * FROM $tableName WHERE data ->> 'id' = ?"""
    private val saveStatement = "INSERT INTO $tableName (data) VALUES (?::json);"
    private val updateStatement = "UPDATE $tableName SET data = ?::json WHERE data ->> 'id' = ?"
    private val deleteStatement = """DELETE FROM $tableName WHERE data ->> 'id' = ?"""
    private val getNextReferanseStatement = "SELECT nextval('referanse_seq')"

    override fun getById(id: UUID): T? {
        ds.connection.use {
            val existingList = ArrayList<T>()
            val res = it.prepareStatement(getByIdStatement).apply {
                setString(1, id.toString())
            }.executeQuery()

            while (res.next()) {
                val sg = mapper.readValue(res.getString("data"), clazz)
                existingList.add(sg)
            }

            return existingList.firstOrNull()
        }
    }

    override fun insert(entity: T, connection: Connection): T {
        val referansenummer = getNextReferanse(connection)
        val entityMedReferansenummer = mapper
            .readValue(mapper.writeValueAsString(entity), ObjectNode::class.java).apply {
                put("referansenummer", referansenummer)
            }
        val json = mapper.writeValueAsString(entityMedReferansenummer)

        connection.prepareStatement(saveStatement).apply {
            setString(1, json)
        }.executeUpdate()
        return entity
    }

    private fun getNextReferanse(connection: Connection): Int? {
        val res = connection.prepareStatement(getNextReferanseStatement).executeQuery()
        if (res.next()) {
            return res.getInt(1)
        }
        return null
    }

    override fun insert(entity: T): T {
        ds.connection.use {
            return insert(entity, it)
        }
    }

    override fun delete(id: UUID, connection: Connection): Int {
        return connection.prepareStatement(deleteStatement).apply {
            setString(1, id.toString())
        }.executeUpdate()
    }

    override fun delete(id: UUID): Int {
        ds.connection.use {
            return delete(id, it)
        }
    }

    override fun update(entity: T, connection: Connection) {
        val json = mapper.writeValueAsString(entity)
        ds.connection.use {
            it.prepareStatement(updateStatement).apply {
                setString(1, json)
                setString(2, entity.id.toString())
            }.executeUpdate()
        }
    }

    override fun update(entity: T) {
        ds.connection.use {
            return update(entity, it)
        }
    }
}
