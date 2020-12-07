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


val INNKOMMENDE_GRAVID_SOEKNADER_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("inkommende_gravis_soeknad")
        .help("Counts the number of incoming applicaitons pregnant")
        .register()