package no.nav.helse.fritakagp.processing.kvittering

import no.nav.helse.fritakagp.domain.SoeknadGravid

class DummyKvitteringSender: KvitteringSender{
    override fun send(kvittering: SoeknadGravid) {
        println("Sender kvittering: ${kvittering.id}")
    }
}