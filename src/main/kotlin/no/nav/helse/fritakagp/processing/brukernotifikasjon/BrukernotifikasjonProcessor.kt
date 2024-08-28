package no.nav.helse.fritakagp.processing.brukernotifikasjon

import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.integration.kafka.BrukernotifikasjonSender
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class BrukernotifikasjonProcessor(
    private val brukerNotifikasjonProducerFactory: BrukernotifikasjonSender,
    private val brukernotifikasjonService: BrukernotifikasjonService
) : BakgrunnsjobbProsesserer {
    override val type: String get() = JOB_TYPE
    private val logger = this.logger()

    companion object {
        val JOB_TYPE = "brukernotifikasjon"
    }

    override fun prosesser(jobb: Bakgrunnsjobb) {
        logger.info("Prosesserer ${jobb.uuid} med type ${jobb.type}")

        val varselId = UUID.randomUUID().toString()
        val varsel = brukernotifikasjonService.opprettVarsel(varselId = varselId, jobb = jobb)
        brukerNotifikasjonProducerFactory.sendMessage(varselId = varselId, varsel = varsel)
    }
}
