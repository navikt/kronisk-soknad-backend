package no.nav.helse.fritakagp.service

import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.fritakagp.domain.GeografiskTilknytningData
import no.nav.helse.fritakagp.integration.norg.ArbeidsfordelingRequest
import no.nav.helse.fritakagp.integration.norg.ArbeidsfordelingResponse
import no.nav.helse.fritakagp.integration.norg.Norg2Client
import no.nav.helsearbeidsgiver.utils.log.logger
import java.time.LocalDate

const val SYKEPENGER_UTLAND = "4474"
const val SYKEPENGER = "SYK"

class BehandlendeEnhetService(
    private val pdlClient: PdlClient,
    private val norg2Client: Norg2Client
) {

    private val logger = this.logger()

    fun hentBehandlendeEnhet(fnr: String, uuid: String, tidspunkt: LocalDate = LocalDate.now()): String {
        val geografiskTilknytning = hentGeografiskTilknytning(fnr)

        val criteria = ArbeidsfordelingRequest(
            tema = SYKEPENGER,
            diskresjonskode = geografiskTilknytning?.diskresjonskode,
            geografiskOmraade = geografiskTilknytning?.geografiskTilknytning
        )

        val callId = MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID)

        try {
            val arbeidsfordelinger = runBlocking {
                norg2Client.hentAlleArbeidsfordelinger(criteria, callId)
            }
            logger.info("Fant enheter: " + arbeidsfordelinger.toString())
            val behandlendeEnhet = finnAktivBehandlendeEnhet(
                arbeidsfordelinger,
                geografiskTilknytning?.geografiskTilknytning,
                tidspunkt
            )

            logger.info("Fant geografiskTilknytning ${geografiskTilknytning.geografiskTilknytning} med behandlendeEnhet $behandlendeEnhet for krav $uuid")
            return behandlendeEnhet
        } catch (e: RuntimeException) {
            logger.error("Klarte ikke å hente behandlende enhet!", e)
            throw BehandlendeEnhetFeiletException(e)
        }
    }

    fun hentGeografiskTilknytning(fnr: String): GeografiskTilknytningData {
        pdlClient.fullPerson(fnr).let {
            return GeografiskTilknytningData(
                geografiskTilknytning = it?.hentGeografiskTilknytning?.hentTilknytning(),
                diskresjonskode = it?.hentPerson?.trekkUtDiskresjonskode()
            )
        }
    }
}

fun finnAktivBehandlendeEnhet(arbeidsfordelinger: List<ArbeidsfordelingResponse>, geografiskTilknytning: String?, tidspunkt: LocalDate): String {
    return arbeidsfordelinger
        .stream()
        .findFirst().orElseThrow { IngenAktivEnhetException(geografiskTilknytning, null) }.enhetNr!!
}

open class BehandlingException(message: String, cause: Exception?) : RuntimeException(message, cause)
open class BehandlendeEnhetException(message: String, cause: Exception?) : BehandlingException(message, cause)
open class IngenAktivEnhetException(geografiskTilknytning: String?, cause: java.lang.Exception?) : BehandlendeEnhetException("Fant ingen aktiv enhet for $geografiskTilknytning", cause)
open class BehandlendeEnhetFeiletException(cause: java.lang.Exception?) : BehandlendeEnhetException("Feil ved henting av geografisk tilknytning", cause)
