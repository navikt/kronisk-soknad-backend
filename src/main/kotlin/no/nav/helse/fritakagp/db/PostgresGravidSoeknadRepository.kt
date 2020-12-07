package no.nav.helse.fritakagp.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.fritakagp.domain.SoeknadGravid
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.*
import javax.sql.DataSource
import kotlin.collections.ArrayList

class PostgresGravidSoeknadRepository(val ds: DataSource, val mapper: ObjectMapper) : GravidSoeknadRepository {
    private val logger = LoggerFactory.getLogger(PostgresGravidSoeknadRepository::class.java)

    private val tableName = "soeknadgravid"

    private val getByIdStatement = """SELECT * FROM $tableName WHERE data ->> 'id' = ?"""

    private val saveStatement = "INSERT INTO $tableName (data) VALUES (?::json);"

    private val updateStatement = "UPDATE $tableName SET data = ?::json WHERE data ->> 'id' = ?"

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

    override fun insert(soeknad: SoeknadGravid, connection: Connection): UUID {
        val json = mapper.writeValueAsString(soeknad)
        connection.prepareStatement(saveStatement).apply {
                setString(1, json)
        }.executeUpdate()
        logger.debug("Har lagt inn søknaden ${soeknad.id}")
        return soeknad.id
    }

    override fun insert(soeknad: SoeknadGravid) : UUID {
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

    override fun update(soeknad: SoeknadGravid) {
        val json = mapper.writeValueAsString(soeknad)
        ds.connection.use {
            it.prepareStatement(updateStatement).apply {
                setString(1, json)
                setString(2, soeknad.id.toString())
            }.executeUpdate()
        }
    }
}