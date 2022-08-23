package no.nav.helse.fritakagp.processing.kronisk.krav

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.nyStatusSak
import org.slf4j.LoggerFactory

class KroniskKravOppdaterNotifikasjonProcessor(
    private val kroniskKravRepo: KroniskKravRepository,
    private val arbeidsgiverNotifikasjonKlient: ArbeidsgiverNotifikasjonKlient,
    private val om: ObjectMapper,
    private val frontendAppBaseUrl: String = "https://arbeidsgiver.nav.no/fritak-agp"
) : BakgrunnsjobbProsesserer {
    companion object {
        val JOB_TYPE = "oppdater-kronsik-krav"
        val dokumentasjonBrevkode = "oppdater_krav_lenke"
    }

    override val type: String get() = JOB_TYPE

    val log = LoggerFactory.getLogger(KroniskKravOppdaterNotifikasjonProcessor::class.java)

    override fun prosesser(jobb: Bakgrunnsjobb) {
        val krav = getOrThrow(jobb)
        log.info("Oppdaterer krav lenke ${krav!!.id}")
        runBlocking {
            arbeidsgiverNotifikasjonKlient.nyStatusSak(
                id = krav.arbeidsgiverSakId!!,
                nyLenkeTilSak = "$frontendAppBaseUrl/nb/kronisk/krav/${krav.id}"
            )
        }
    }

    private fun getOrThrow(jobb: Bakgrunnsjobb): KroniskKrav? {
        val jobbData = om.readValue<KroniskKravProcessor.JobbData>(jobb.data)
        val krav = kroniskKravRepo.getById(jobbData.id)
        requireNotNull(krav, { "Jobben indikerte et krav med id ${jobb.data} men den kunne ikke finnes" })
        return krav
    }
}
