package no.nav.helse.fritakagp.processing.kvittering

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage
import no.nav.helse.fritakagp.domain.SoeknadGravid
import org.slf4j.LoggerFactory

class AltinnKvitteringSender(
        private val altinnKvitteringMapper: AltinnKvitteringMapper,
        private val iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
        private val username: String,
        private val password: String) : KvitteringSender {

    private val log = LoggerFactory.getLogger("AltinnKvitteringSender")
    private val FEIL_VED_SENDING = "Feil ved sending kvittering til Altinn"

    companion object {
        const val SYSTEM_USER_CODE = "NAV_HELSEARBEIDSGIVER"
    }

    override fun send(kvittering: SoeknadGravid) {
        try {
            val receiptExternal = iCorrespondenceAgencyExternalBasic.insertCorrespondenceBasicV2(
                    username, password,
                    SYSTEM_USER_CODE, kvittering.id.toString(),
                    altinnKvitteringMapper.mapKvitteringTilInsertCorrespondence(kvittering)
            )
            if (receiptExternal.receiptStatusCode != ReceiptStatusEnum.OK) {
                log.error("Fikk uventet statuskode fra Altinn {}", receiptExternal.receiptStatusCode)
                throw RuntimeException(FEIL_VED_SENDING)
            }
        } catch (e: ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage) {
            log.error(FEIL_VED_SENDING, e)
            log.error("${e.faultInfo} ${e.cause} ${e.message}")
            log.error(FEIL_VED_SENDING, e)
            throw RuntimeException(FEIL_VED_SENDING, e)
        } catch (e: Exception) {
            log.error(FEIL_VED_SENDING, e)
            throw e
        }
    }
}