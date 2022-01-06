package no.nav.helse.fritakagp.service

import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient

class PdlService(val pdlClient: PdlClient) {
    fun finnNavn(fnr: String): String {
        val navn = pdlClient.personNavn(fnr)?.navn?.firstOrNull()
        return if (navn?.mellomnavn.isNullOrEmpty()) {
            "${navn?.fornavn} ${navn?.etternavn}"
        } else {
            "${navn?.fornavn} ${navn?.mellomnavn} ${navn?.etternavn}"
        }
    }
}
