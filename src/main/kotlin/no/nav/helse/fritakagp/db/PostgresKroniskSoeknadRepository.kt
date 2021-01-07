package no.nav.helse.fritakagp.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.fritakagp.domain.SoeknadKronisk
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.*
import javax.sql.DataSource
import kotlin.collections.ArrayList

class PostgresKroniskSoeknadRepository(val ds: DataSource, val mapper: ObjectMapper) : KroniskSoeknadRepository {
    private val logger = LoggerFactory.getLogger(PostgresKroniskSoeknadRepository::class.java)

    private val tableName = "soeknadkronisk"

    private val getByIdStatement = """SELECT * FROM $tableName WHERE data ->> 'id' = ?"""

    private val saveStatement = "INSERT INTO $tableName (data) VALUES (?::json);"

    private val updateStatement = "UPDATE $tableName SET data = ?::json WHERE data ->> 'id' = ?"

    private val deleteStatement = """DELETE FROM $tableName WHERE data ->> 'id' = ?"""

    override fun getById(id: UUID): SoeknadKronisk? {
        ds.connection.use {
            val existingList = ArrayList<SoeknadKronisk>()
            val res = it.prepareStatement(getByIdStatement).apply {
                setString(1,id.toString())
            }.executeQuery()

            while (res.next()) {
                val sg = mapper.readValue<SoeknadKronisk>(res.getString("data"))
                existingList.add(sg)
            }

            return existingList.firstOrNull()
        }
    }

    override fun insert(soeknad: SoeknadKronisk, connection: Connection): UUID {
        val json = mapper.writeValueAsString(soeknad)
        connection.prepareStatement(saveStatement).apply {
                setString(1, json)
        }.executeUpdate()
        logger.debug("Har lagt inn kronisksøknaden ${soeknad.id}")
        return soeknad.id
    }

    override fun insert(soeknad: SoeknadKronisk) : UUID {
        ds.connection.use {
            insert(soeknad, it)
        }
        return soeknad.id
    }

    override fun delete(id: UUID): Int {
        ds.connection.use {
            return it.prepareStatement(deleteStatement).apply {
                setString(1, id.toString())
            }.executeUpdate()
        }
    }

    override fun update(soeknad: SoeknadKronisk) {
        val json = mapper.writeValueAsString(soeknad)
        ds.connection.use {
            it.prepareStatement(updateStatement).apply {
                setString(1, json)
                setString(2, soeknad.id.toString())
            }.executeUpdate()
        }
    }
}