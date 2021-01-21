package no.nav.helse.fritakagp

import io.prometheus.client.Counter
import io.prometheus.client.Summary
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsvarsler

const val METRICS_NS = "fritakagp"


class MetrikkVarsler : Bakgrunnsvarsler {
    override fun rapporterPermanentFeiletJobb() {
        FEILET_JOBB_COUNTER.inc()
    }
}

val FEILET_JOBB_COUNTER = Counter.build()
        .namespace(METRICS_NS)
        .name("feilet_jobb")
        .help("Counts the number of permanently failed jobs")
        .register()


object GravidKravMetrics :
    ProseseringsMetrikker("gravid_krav", "Metrikker for krav, gravid")

object KroniskKravMetrics :
    ProseseringsMetrikker("kronisk_krav", "Metrikker for krav, kronisk")

object GravidSoeknadMetrics :
    ProseseringsMetrikker("gravid_soeknad", "Metrikker for søknader, gravid")

object KroniskSoeknadMetrics :
    ProseseringsMetrikker("kronisk_soeknad", "Metrikker for søknader, kronisk")



abstract class ProseseringsMetrikker(metricName: String, metricHelpText: String) {
    private val GRAVID_SOEKNAD: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(metricName)
        .labelNames("hendelse")
        .help(metricHelpText)
        .register()

    fun tellMottatt() = GRAVID_SOEKNAD.labels("mottatt").inc()
    fun tellJournalfoert() = GRAVID_SOEKNAD.labels("journalfoert").inc()
    fun tellOppgaveOpprettet() = GRAVID_SOEKNAD.labels("oppgaveOpprettet").inc()
    fun tellKvitteringSendt() = GRAVID_SOEKNAD.labels("kvitteringSendt").inc()
}