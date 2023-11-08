package no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.BakgrunnsjobbProsesserer
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.enums.SaksStatus
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

/* One-off-job som skal oppdatere alle saker med gammel status "under behandling" til ny status "MOTTATT"
* Når alle disse jobbtypene er utført (eller har feilet for godt), kan denne klassen slettes - sjekk det med sql:
* select status, count(status)
* from bakgrunnsjobb
     where type = 'arbeidsgiveroppdaternotifikasjon'
     group by status
*
*/
class ArbeidsgiverOppdaterNotifikasjonProcessor(
    private val om: ObjectMapper,
    private val arbeidsgiverNotifikasjonKlient: ArbeidsgiverNotifikasjonKlient
) : BakgrunnsjobbProsesserer {
    private val logger = this.logger()

    companion object {
        const val JOB_TYPE = "arbeidsgiveroppdaternotifikasjon"
    }

    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        logger.info("Prosesserer ${jobb.uuid} med type ${jobb.type}")
        val jobbData = om.readValue<JobbData>(jobb.data)
        val resultat = runBlocking {
            arbeidsgiverNotifikasjonKlient.nyStatusSakByGrupperingsid(
                grupperingsid = jobbData.skjemaId.toString(),
                merkelapp = "Fritak arbeidsgiverperiode",
                nyStatus = SaksStatus.MOTTATT
            )
        }
        logger.info("Oppdaterte sak med ${jobbData.skjemaId} med ref $resultat")
    }

    data class JobbData(
        val skjemaId: UUID,
        val skjemaType: SkjemaType
    ) {
        enum class SkjemaType {
            KroniskKrav,
            GravidKrav
        }
    }
}
