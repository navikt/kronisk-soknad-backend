package no.nav.helse.fritakagp.db

import javax.sql.DataSource

data class WeeklyStats(
    val uke: Int,
    val antall: Int,
    val tabell: String
)
interface IStatsRepo {
    fun getWeeklyStats(): List<WeeklyStats>
}

class StatsRepoImpl(
    private val ds: DataSource
) : IStatsRepo {
    override fun getWeeklyStats(): List<WeeklyStats> {
        val query = """
            select
                extract('week' from date(data->>'opprettet')) as uke,
                count(*) as antall,
                'gravid_soeknad' as table_name
            from soeknadgravid
            group by uke
            UNION ALL
            select
                extract('week' from date(data->>'opprettet')) as uke,
                count(*) as antall,
                'kronisk_soeknad' as table_name
            from soeknadkronisk
            group by uke
            UNION ALL
            select
                extract('week' from date(data->>'opprettet')) as uke,
                count(*) as antall,
                'kronisk_krav' as table_name
            from krav_kronisk
            group by uke
            UNION ALL
            select
                extract('week' from date(data->>'opprettet')) as uke,
                count(*) as antall,
                'gravid_krav' as table_name
            from kravgravid
            group by uke
            order by uke;
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<WeeklyStats>()
            while (res.next()){
                returnValue.add(
                    WeeklyStats(
                        res.getInt("uke"),
                        res.getInt("antall"),
                        res.getString("table_name")
                    )
                )
            }

            return returnValue
        }
    }
}
