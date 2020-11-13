package no.nav.helse.fritakagp.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.fritakagp.domain.SoeknadGravid
import org.slf4j.LoggerFactory
import java.io.IOException
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource
import kotlin.collections.ArrayList

class PostgresRepository(val ds: DataSource, val mapper: ObjectMapper) : Repository {
    private val logger = LoggerFactory.getLogger(PostgresRepository::class.java)

    private val tableName = "soeknadgravid"

    private val getByIdStatement = """SELECT * FROM $tableName WHERE data ->> 'id' = ?"""

    private val saveStatement = "INSERT INTO $tableName (data) VALUES (?::json);"

    private val deleteStatement = """DELETE FROM $tableName WHERE data ->> 'id' = ?"""


    override fun getById(id: UUID): SoeknadGravid? {
        ds.connection.use {
            val existingList = ArrayList<SoeknadGravid>()
            val res = it.prepareStatement(getByIdStatement).apply {
                setString(1,id.toString())
            }.executeQuery()

            while (res.next()) {
                val sg = mapper.readValue<SoeknadGravid>(res.getString("data"))
                existingList.add(sg)
            }

            return existingList.firstOrNull()
        }
    }

    override fun insert(soeknad: SoeknadGravid) : UUID {
        val json = mapper.writeValueAsString(soeknad)
        ds.connection.use {
            it.prepareStatement(saveStatement).apply {
                setString(1, json)
            }.executeUpdate()
        }
        logger.info("Har lagt inn søknaden ${soeknad.id}")
        return soeknad.id
    }

    override fun delete(id: UUID): Int {
        ds.connection.use {
            return it.prepareStatement(deleteStatement).apply {
                setString(1, id.toString())
            }.executeUpdate()
        }
    }
}