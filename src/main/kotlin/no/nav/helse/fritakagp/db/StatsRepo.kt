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

data class AntallType(
    val antall: Int,
    val type: String
)

data class SykeGradAntall(
    val antall: Int,
    val bucket: Int,
    val uke: Int
)

interface IStatsRepo {
    fun getWeeklyStats(): List<WeeklyStats>
    fun getGravidSoeknadTiltak(): GravidSoeknadTiltak
    fun getKroniskSoeknadArbeidstyper(): List<AntallType>
    fun getKroniskSoeknadPaakjenningstyper(): List<AntallType>
    fun getSykeGradAntall(): List<SykeGradAntall>
}

class StatsRepoImpl(
    private val ds: DataSource
) : IStatsRepo {
    override fun getWeeklyStats(): List<WeeklyStats> {
        val query = """
            select
                extract('week' from date_trunc('week', date(data->>'opprettet'))) as uke,
                extract('year' from date_trunc('week', date(data->>'opprettet'))) as year,
                count(*) as antall,
                'gravid_soeknad' as table_name
            from soeknadgravid
            group by year, uke
            UNION ALL
            select
                extract('week' from date_trunc('week', date(data->>'opprettet'))) as uke,
                extract('year' from date_trunc('week', date(data->>'opprettet'))) as year,
                count(*) as antall,
                'kronisk_soeknad' as table_name
            from soeknadkronisk
            group by year, uke
            UNION ALL
            select
                extract('week' from date_trunc('week', date(data->>'opprettet'))) as uke,
                extract('year' from date_trunc('week', date(data->>'opprettet'))) as year,
                count(*) as antall,
                'kronisk_krav' as table_name
            from krav_kronisk
            group by year, uke
            UNION ALL
            select
                extract('week' from date_trunc('week', date(data->>'opprettet'))) as uke,
                extract('year' from date_trunc('week', date(data->>'opprettet'))) as year,
                count(*) as antall,
                'gravid_krav' as table_name
            from kravgravid
            group by year, uke
            order by year, uke;
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<WeeklyStats>()
            while (res.next()) {
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
            count (*) filter (where(data->'tiltak')::jsonb ?? 'HJEMMEKONTOR') as hjemmekontor,
            count (*) filter (where(data->'tiltak')::jsonb ?? 'TILPASSEDE_ARBEIDSOPPGAVER') as tilpassede_arbeidsoppgaver,
            count (*) filter (where(data->'tiltak')::jsonb ?? 'TILPASSET_ARBEIDSTID') as tilpasset_arbeidstid,
            count (*) filter (where(data->'tiltak')::jsonb ?? 'ANNET') as annet
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

    override fun getKroniskSoeknadArbeidstyper(): List<AntallType> {
        val query = """
        SELECT
            count(*) as antall,
            jsonb_array_elements(k.data->'arbeidstyper') as arbeidstype
        FROM soeknadkronisk as k
        WHERE date(data->>'opprettet') > NOW()::DATE-EXTRACT(DOW FROM NOW())::INTEGER-7
        GROUP BY arbeidstype;
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<AntallType>()
            while (res.next()) {
                returnValue.add(
                    AntallType(
                        res.getInt("antall"),
                        res.getString("arbeidstype")
                    )
                )
            }

            return returnValue
        }
    }
    override fun getKroniskSoeknadPaakjenningstyper(): List<AntallType> {
        val query = """
        SELECT
            count(*) as antall,
            jsonb_array_elements(k.data->'paakjenningstyper') as paakjenning
        FROM soeknadkronisk as k
        WHERE date(data->>'opprettet') > NOW()::DATE-EXTRACT(DOW FROM NOW())::INTEGER-7
        GROUP BY paakjenning;
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<AntallType>()
            while (res.next()) {
                returnValue.add(
                    AntallType(
                        res.getInt("antall"),
                        res.getString("paakjenning")
                    )
                )
            }
            return returnValue
        }
    }

    override fun getSykeGradAntall(): List<SykeGradAntall> {
        val query = """
            SELECT count(bucket) as antall, bucket, uke
            FROM (
                SELECT width_bucket((json_array_elements((data#>'{perioder}')::json)->>'gradering')::float, 0.0, 1.0, 5) as bucket,
                    extract('week' from date(data->>'opprettet')) as uke
                FROM krav_kronisk
                WHERE (data->>'opprettet')::DATE >  NOW()::DATE - INTERVAL '90 DAYS'
                ) AS temp GROUP BY bucket, uke;
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<SykeGradAntall>()
            while (res.next()) {
                returnValue.add(
                    SykeGradAntall(
                        res.getInt("antall"),
                        res.getInt("bucket"),
                        res.getInt("uke")
                    )
                )
            }
            return returnValue
        }
    }
}
