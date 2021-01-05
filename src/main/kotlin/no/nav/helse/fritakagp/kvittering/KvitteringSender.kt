package no.nav.helse.fritakagp.kvittering

import no.nav.helse.fritakagp.processing.kvittering.Kvittering

interface KvitteringSender {
    fun send(kvittering: Kvittering)
}