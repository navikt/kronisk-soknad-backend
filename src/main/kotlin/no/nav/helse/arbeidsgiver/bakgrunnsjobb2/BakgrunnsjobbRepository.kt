package no.nav.helse.arbeidsgiver.bakgrunnsjobb2

import no.nav.helse.arbeidsgiver.processing.AutoCleanJobbProcessor
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

interface BakgrunnsjobbRepository {
    fun getById(id: UUID): Bakgrunnsjobb?
    fun save(bakgrunnsjobb: Bakgrunnsjobb)
    fun save(bakgrunnsjobb: Bakgrunnsjobb, connection: Connection)
    fun findAutoCleanJobs(): List<Bakgrunnsjobb>
    fun findOkAutoCleanJobs(): List<Bakgrunnsjobb>
    fun findByKjoeretidBeforeAndStatusIn(timeout: LocalDateTime, tilstander: Set<BakgrunnsjobbStatus>, alle: Boolean): List<Bakgrunnsjobb>
    fun delete(uuid: UUID)
    fun deleteAll()
    fun deleteOldOkJobs(months: Long)
    fun update(bakgrunnsjobb: Bakgrunnsjobb)
    fun update(bakgrunnsjobb: Bakgrunnsjobb, connection: Connection)
}

class MockBakgrunnsjobbRepository : BakgrunnsjobbRepository {

    private val jobs = mutableListOf<Bakgrunnsjobb>()

    override fun getById(id: UUID): Bakgrunnsjobb? {
        if (jobs.filter { it.uuid.equals(id) }.size.equals(1)) {
            return jobs.filter { it.uuid.equals(id) }.get(0)
        } else {
            return null
        }
    }

    override fun save(bakgrunnsjobb: Bakgrunnsjobb) {
        jobs.add(bakgrunnsjobb)
    }

    override fun save(bakgrunnsjobb: Bakgrunnsjobb, connection: Connection) {
        jobs.add(bakgrunnsjobb)
    }

    override fun findAutoCleanJobs(): List<Bakgrunnsjobb> {
        return jobs.filter { it.type.equals(AutoCleanJobbProcessor.JOB_TYPE) }
    }

    override fun findOkAutoCleanJobs(): List<Bakgrunnsjobb> {
        return jobs.filter { it.type.equals(AutoCleanJobbProcessor.JOB_TYPE) }
    }

    override fun findByKjoeretidBeforeAndStatusIn(timeout: LocalDateTime, tilstander: Set<BakgrunnsjobbStatus>, alle: Boolean): List<Bakgrunnsjobb> {
        return jobs.filter { tilstander.contains(it.status) }
            .filter { it.kjoeretid.isBefore(timeout) }
    }

    override fun delete(uuid: UUID) {
        jobs.removeIf { it.uuid == uuid }
    }

    override fun deleteAll() {
        jobs.removeAll { true }
    }

    override fun update(bakgrunnsjobb: Bakgrunnsjobb) {
        delete(bakgrunnsjobb.uuid)
        save(bakgrunnsjobb)
    }

    override fun update(bakgrunnsjobb: Bakgrunnsjobb, connection: Connection) {
        update(bakgrunnsjobb)
    }

    override fun deleteOldOkJobs(months: Long) {
        val someMonthsAgo = LocalDateTime.now().minusMonths(months)
        jobs.removeIf { it.behandlet?.isBefore(someMonthsAgo)!! && it.status.equals(BakgrunnsjobbStatus.OK) }
    }
}

class PostgresBakgrunnsjobbRepository(val dataSource: DataSource) : BakgrunnsjobbRepository {
    private val tableName = "bakgrunnsjobb"

    private val insertStatement =
        """INSERT INTO $tableName (jobb_id, type, behandlet, opprettet, status, kjoeretid, forsoek, maks_forsoek, data) VALUES (?::uuid,?,?,?,?,?,?,?,?::json)""".trimIndent()

    private val updateStatement = """UPDATE $tableName
        SET behandlet = ?
         , status = ?
         , kjoeretid = ?
         , forsoek = ?
         , data = ?::json
        where jobb_id = ?::uuid"""
        .trimIndent()

    private val selectStatement = """
        select * from $tableName where kjoeretid < ? and status = ANY(?)
    """.trimIndent()

    private val selectStatementWithLimit = selectStatement + " limit 100".trimIndent()

    private val selectByIdStatement = """select * from $tableName where jobb_id = ?""".trimIndent()

    private val selectAutoClean =
        """SELECT * from $tableName WHERE status IN ('${BakgrunnsjobbStatus.OPPRETTET}','${BakgrunnsjobbStatus.FEILET}') AND type = '${AutoCleanJobbProcessor.JOB_TYPE}'""".trimIndent()

    private val selectOkAutoClean =
        """SELECT * from $tableName WHERE status = '${BakgrunnsjobbStatus.OK}' AND type = '${AutoCleanJobbProcessor.JOB_TYPE}'""".trimIndent()

    private val deleteStatement = "DELETE FROM $tableName where jobb_id = ?::uuid"

    private val deleteOldJobsStatement = """DELETE FROM $tableName WHERE status = '${BakgrunnsjobbStatus.OK}' AND behandlet < ?""".trimIndent()

    private val deleteAllStatement = "DELETE FROM $tableName"

    override fun getById(id: UUID): Bakgrunnsjobb? {
        dataSource.connection.use {
            return getById(id, it)
        }
    }

    fun getById(id: UUID, connection: Connection): Bakgrunnsjobb? {
        connection.prepareStatement("select * from $tableName where jobb_id = '$id'").use {
            val rs = it.executeQuery()
            val resultList = resultsetTilResultatliste(rs)
            if (resultList.size == 0) {
                return null
            } else {
                return resultList[0]
            }
        }
    }

    override fun save(bakgrunnsjobb: Bakgrunnsjobb) {
        dataSource.connection.use {
            save(bakgrunnsjobb, it)
        }
    }

    override fun save(bakgrunnsjobb: Bakgrunnsjobb, connection: Connection) {
        connection.prepareStatement(insertStatement).apply {
            setString(1, bakgrunnsjobb.uuid.toString())
            setString(2, bakgrunnsjobb.type)
            setTimestamp(3, bakgrunnsjobb.behandlet?.let(Timestamp::valueOf))
            setTimestamp(4, Timestamp.valueOf(bakgrunnsjobb.opprettet))
            setString(5, bakgrunnsjobb.status.toString())
            setTimestamp(6, Timestamp.valueOf(bakgrunnsjobb.kjoeretid))
            setInt(7, bakgrunnsjobb.forsoek)
            setInt(8, bakgrunnsjobb.maksAntallForsoek)
            setString(9, bakgrunnsjobb.data)
        }.use {
            it.execute()
        }
    }

    override fun update(bakgrunnsjobb: Bakgrunnsjobb) {
        dataSource.connection.use {
            update(bakgrunnsjobb, it)
        }
    }

    override fun update(bakgrunnsjobb: Bakgrunnsjobb, connection: Connection) {
        connection.prepareStatement(updateStatement).apply {
            setTimestamp(1, bakgrunnsjobb.behandlet?.let(Timestamp::valueOf))
            setString(2, bakgrunnsjobb.status.toString())
            setTimestamp(3, Timestamp.valueOf(bakgrunnsjobb.kjoeretid))
            setInt(4, bakgrunnsjobb.forsoek)
            setString(5, bakgrunnsjobb.data)
            setString(6, bakgrunnsjobb.uuid.toString())
        }.use {
            it.executeUpdate()
        }
    }

    override fun findAutoCleanJobs(): List<Bakgrunnsjobb> {
        dataSource.connection.use { con ->
            con.prepareStatement(selectAutoClean).use {
                val res = it.executeQuery()
                return resultsetTilResultatliste(res)
            }
        }
    }

    override fun findOkAutoCleanJobs(): List<Bakgrunnsjobb> {
        dataSource.connection.use { con ->
            con.prepareStatement(selectOkAutoClean).use {
                val res = it.executeQuery()
                return resultsetTilResultatliste(res)
            }
        }
    }

    override fun findByKjoeretidBeforeAndStatusIn(timeout: LocalDateTime, tilstander: Set<BakgrunnsjobbStatus>, alle: Boolean): List<Bakgrunnsjobb> {
        val selectString = if (alle) {
            selectStatement
        } else {
            selectStatementWithLimit
        }
        dataSource.connection.use { con ->
            con.prepareStatement(selectString).apply {
                setTimestamp(1, Timestamp.valueOf(timeout))
                setArray(2, con.createArrayOf("VARCHAR", tilstander.map { it.toString() }.toTypedArray()))
            }.use {
                val res = it.executeQuery()
                return resultsetTilResultatliste(res)
            }
        }
    }

    private fun resultsetTilResultatliste(res: ResultSet): MutableList<Bakgrunnsjobb> {
        val resultatListe = mutableListOf<Bakgrunnsjobb>()
        res.use {
            while (it.next()) {
                resultatListe.add(
                    Bakgrunnsjobb(
                        UUID.fromString(it.getString("jobb_id")),
                        it.getString("type"),
                        it.getTimestamp("behandlet")?.toLocalDateTime(),
                        it.getTimestamp("opprettet").toLocalDateTime(),
                        BakgrunnsjobbStatus.valueOf(it.getString("status")),
                        it.getTimestamp("kjoeretid").toLocalDateTime(),
                        it.getInt("forsoek"),
                        it.getInt("maks_forsoek"),
                        it.getString("data")
                    )
                )
            }
        }
        return resultatListe
    }

    override fun delete(uuid: UUID) {
        dataSource.connection.use { con ->
            con.prepareStatement(deleteStatement).apply {
                setString(1, uuid.toString())
            }.use {
                it.executeUpdate()
            }
        }
    }

    override fun deleteAll() {
        dataSource.connection.use { con ->
            con.prepareStatement(deleteAllStatement).use {
                it.executeUpdate()
            }
        }
    }

    override fun deleteOldOkJobs(months: Long) {
        dataSource.connection.use { con ->
            con.prepareStatement(deleteOldJobsStatement).apply {
                setDate(1, Date.valueOf(LocalDate.now().minusMonths(months)))
            }.use {
                it.executeUpdate()
            }
        }
    }
}
