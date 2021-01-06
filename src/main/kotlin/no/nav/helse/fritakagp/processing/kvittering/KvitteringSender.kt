package no.nav.helse.fritakagp.processing.kvittering

import no.nav.helse.fritakagp.domain.SoeknadGravid

interface KvitteringSender {
    fun send(kvittering: SoeknadGravid)
}