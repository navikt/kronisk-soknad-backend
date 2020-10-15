package no.nav.helse.fritakagp.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.io.IOException
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.collections.ArrayList

class PostgresRepository(val ds: DataSource, val mapper: ObjectMapper) : Repository {
    private val logger = LoggerFactory.getLogger(PostgresRepository::class.java)

    private val tableName = "test"

    private val getByIdStatement = """SELECT * FROM $tableName WHERE id = ?"""

    private val saveStatement = "INSERT INTO $tableName (id, data) VALUES (?, ?::json);"

    private val deleteStatement = "DELETE FROM $tableName WHERE id = ?"


    override fun getById(id: Int): String? {
        ds.connection.use {
            val existingList = ArrayList<String>()
            val res = it.prepareStatement(getByIdStatement).apply {
                setString(1, id.toString())
            }.executeQuery()

            while (res.next()) {
                existingList.add(extractString(res))
            }

            return existingList.firstOrNull()
        }
    }

    override fun insert(testString: String, id: Int): String {
        val json = """ {'tekst' : '$testString'}"""
        ds.connection.use {
            it.prepareStatement(saveStatement).apply {
                setInt(1, id)
                setString(2, json)
            }.executeUpdate()
        }
        return getById(id)
                ?: throw IOException("Fant ikke $id")
    }


    override fun delete(id: Int): Int {
        ds.connection.use {
            return it.prepareStatement(deleteStatement).apply {
                setString(1, id.toString())
            }.executeUpdate()
        }
    }


    private fun extractString(res: ResultSet): String {
        val testString = mapper.readValue<TestString>(res.getString("data"))
        return testString.tekst
    }
}

data class TestString(
        val tekst: String
)
