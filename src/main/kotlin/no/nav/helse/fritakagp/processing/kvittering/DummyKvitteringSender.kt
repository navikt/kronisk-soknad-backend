package no.nav.helse.fritakagp.processing.kvittering

class DummyKvitteringSender: KvitteringSender{
    override fun send(kvittering: Kvittering) {
        println("Sender kvittering: ${kvittering.id}")
    }
}