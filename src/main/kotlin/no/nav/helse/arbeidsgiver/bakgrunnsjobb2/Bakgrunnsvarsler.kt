package no.nav.helse.arbeidsgiver.bakgrunnsjobb2

interface Bakgrunnsvarsler {

    fun rapporterPermanentFeiletJobb()
}

class TomVarsler() : Bakgrunnsvarsler {
    override fun rapporterPermanentFeiletJobb() {
    }
}
