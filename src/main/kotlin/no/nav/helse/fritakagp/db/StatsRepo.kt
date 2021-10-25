package no.nav.helse.fritakagp.db

import javax.sql.DataSource

data class WeeklyStats(
    val uke: Int,
    val antall: Int,
    val tabell: String
)
data class GravidSoeknadTiltak(
    val hjemmekontor: Int,
    val tilpassede_arbeidsoppgaver: Int,
    val tipasset_arbeidstid: Int,
    val annet: Int,
)
interface IStatsRepo {
    fun getWeeklyStats(): List<WeeklyStats>
    fun getGravidSoeknadTiltak(): GravidSoeknadTiltak
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
            --where uke > extract('week' from DATE::now())) - 12
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

    override fun getGravidSoeknadTiltak(): GravidSoeknadTiltak {
        val query = """
            select
                   count (*) filter (where(data->'tiltak')::jsonb ? 'HJEMMEKONTOR') as hjemmekontor,
                   count (*) filter (where(data->'tiltak')::jsonb ? 'TILPASSEDE_ARBEIDSOPPGAVER') as tilpassede_arbeidsoppgaver,
                   count (*) filter (where(data->'tiltak')::jsonb ? 'TILPASSET_ARBEIDSTID') as tipasset_arbeidstid,
                   count (*) filter (where(data->'tiltak')::jsonb ? 'ANNET') as annet
            from soeknadgravid
            where date(data->>'opprettet') > NOW()::DATE-EXTRACT(DOW FROM NOW())::INTEGER-7;
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<GravidSoeknadTiltak>()
            while (res.next()) {
                returnValue.add(
                    GravidSoeknadTiltak(
                        res.getInt("hjemmekontor"),
                        res.getInt("tilpassede_arbeidsoppgaver"),
                        res.getInt("tilpasset_arbeidstid"),
                        res.getInt("annet"),
                    )
                )
            }

            return returnValue[0]
        }
    }



}
