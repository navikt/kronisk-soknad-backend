package no.nav.helse.fritakagp.db

import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

interface ReferanseRepository {
    fun getOrInsertReferanse(id: UUID): Int
    fun delete(id: UUID): Int
}

class ReferanseRepositoryImpl(val ds: DataSource) : ReferanseRepository {
    private val tableName = "referanse"

    private val getReferanseByUUIDStatement = """SELECT referansenummer FROM $tableName WHERE uuid = (?::uuid) LIMIT 1;"""
    private val insertUUIDStatement = """INSERT INTO $tableName (uuid) VALUES (?::uuid) returning referansenummer;"""
    private val deleteStatement = """DELETE FROM $tableName WHERE uuid = (?::uuid)"""

    override fun getOrInsertReferanse(id: UUID): Int {
        ds.connection.use { con ->
            val res = con.prepareStatement(getReferanseByUUIDStatement).apply {
                setString(1, id.toString())
            }.executeQuery()
            if (res.next()) {
                return res.getInt("referansenummer")
            }
            return insertUUID(id)
        }
    }

    private fun insertUUID(id: UUID): Int {
        ds.connection.use { con ->
            val res = con.prepareStatement(insertUUIDStatement).apply {
                setString(1, id.toString())
            }.executeQuery()
            if (res.next()) {
                return res.getInt("referansenummer")
            } else {
                throw SQLException("Insert referansenummer failed. This should not have happened")
            }
        }
    }

    override fun delete(id: UUID): Int {
        ds.connection.use { con ->
            return con.prepareStatement(deleteStatement).apply {
                setString(1, id.toString())
            }.executeUpdate()
        }
    }
}
