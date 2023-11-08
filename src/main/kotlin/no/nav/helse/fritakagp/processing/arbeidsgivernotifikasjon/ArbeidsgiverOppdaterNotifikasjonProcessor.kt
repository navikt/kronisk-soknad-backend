package no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb2.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.customObjectMapper
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
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
    private val gravidKravRepo: GravidKravRepository,
    private val kroniskKravRepo: KroniskKravRepository,
    private val arbeidsgiverNotifikasjonKlient: ArbeidsgiverNotifikasjonKlient
) : BakgrunnsjobbProsesserer {
    private val logger = this.logger()
    private val objectMapper = customObjectMapper()
    companion object {
        const val JOB_TYPE = "arbeidsgiveroppdaternotifikasjon"
    }

    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        logger.info("Prosesserer ${jobb.uuid} med type ${jobb.type}")
        val jobbData = objectMapper.readValue<JobbData>(jobb.data)
        val sak = map(jobbData)
        val resultat = runBlocking {
            arbeidsgiverNotifikasjonKlient.nyStatusSakByGrupperingsid(
                grupperingsid = sak.id.toString(),
                merkelapp = "Fritak arbeidsgiverperiode",
                nyStatus = SaksStatus.MOTTATT
            )
        }
        logger.info("Oppdaterte sak med ${sak.id} med ref $resultat")
    }

    private fun map(jobbData: JobbData): SakParametere {
        if (jobbData.skjemaType == JobbData.SkjemaType.KroniskKrav) {
            val skjema = kroniskKravRepo.getById(jobbData.skjemaId)
                ?: throw IllegalArgumentException("Fant ikke $jobbData")
            return SakParametere(
                skjema.id
            )
        } else {
            val skjema = gravidKravRepo.getById(jobbData.skjemaId)
                ?: throw IllegalArgumentException("Fant ikke $jobbData")
            return SakParametere(
                skjema.id
            )
        }
    }

    data class SakParametere(
        val id: UUID
    )

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
