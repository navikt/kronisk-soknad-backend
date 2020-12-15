package no.nav.helse.fritakagp.processing.kvittering

interface KvitteringSender {
    fun send(kvittering: Kvittering)
}