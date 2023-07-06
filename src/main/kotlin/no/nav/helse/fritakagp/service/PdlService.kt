package no.nav.helse.fritakagp.service

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.helsearbeidsgiver.pdl.PdlIdent

class PdlService(
    private val pdlClient: PdlClient
) {
    fun hentNavn(ident: String): String {
        val personNavn = runBlocking { pdlClient.personNavn(ident) }
            ?.navn
            ?.firstOrNull()

        return listOf(
            personNavn?.fornavn,
            personNavn?.mellomnavn,
            personNavn?.etternavn,
        )
            .filterNot(String?::isNullOrEmpty)
            .joinToString(" ")
    }

    fun hentAktoerId(ident: String): String? =
        runBlocking { pdlClient.fullPerson(ident) }
            ?.hentIdenter
            ?.trekkUtIdent(PdlIdent.PdlIdentGruppe.AKTORID)

    fun hentGeografiskTilknytning(ident: String): GeografiskTilknytning =
        runBlocking { pdlClient.fullPerson(ident) }
            .let {
                GeografiskTilknytning(
                    diskresjonskode = it?.hentPerson?.trekkUtDiskresjonskode(),
                    geografiskTilknytning = it?.hentGeografiskTilknytning?.hentTilknytning(),
                )
            }
}

data class GeografiskTilknytning(
    var diskresjonskode: String?,
    var geografiskTilknytning: String?,
)
