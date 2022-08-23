package no.nav.helse.fritakagp.processing.gravid.krav

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.nyStatusSak
import org.slf4j.LoggerFactory

class GravidKravOppdaterNotifikasjonProcessor(
    private val gravidKravRepo: GravidKravRepository,
    private val arbeidsgiverNotifikasjonKlient: ArbeidsgiverNotifikasjonKlient,
    private val om: ObjectMapper,
    private val frontendAppBaseUrl: String = "https://arbeidsgiver.nav.no/fritak-agp"
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "oppdater-gravid-krav"
        val dokumentasjonBrevkode = "oppdater_krav_lenke"
    }

    override val type: String get() = SlettGravidKravProcessor.JOB_TYPE

    val log = LoggerFactory.getLogger(GravidKravOppdaterNotifikasjonProcessor::class.java)

    override fun prosesser(jobb: Bakgrunnsjobb) {
        val krav = getOrThrow(jobb)
        log.info("Oppdaterer krav lenke ${krav!!.id}")
        runBlocking {
            arbeidsgiverNotifikasjonKlient.nyStatusSak(
                id = krav.arbeidsgiverSakId!!,
                nyLenkeTilSak = "$frontendAppBaseUrl/nb/gravid/krav/${krav.id}"
            )
        }
    }

    private fun getOrThrow(jobb: Bakgrunnsjobb): GravidKrav? {
        val jobbData = om.readValue<GravidKravProcessor.JobbData>(jobb.data)
        val krav = gravidKravRepo.getById(jobbData.id)
        requireNotNull(krav, { "Jobben indikerte et krav med id ${jobb.data} men den kunne ikke finnes" })
        return krav
    }
}
