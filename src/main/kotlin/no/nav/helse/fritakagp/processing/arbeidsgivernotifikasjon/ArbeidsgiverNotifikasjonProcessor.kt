package no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class ArbeidsgiverNotifikasjonProcessor(
    private val gravidKravRepo: GravidKravRepository,
    private val kroniskKravRepo: KroniskKravRepository,
    private val om: ObjectMapper,
    private val frontendAppBaseUrl: String = "https://arbeidsgiver.nav.no/fritak-agp",
    private val arbeidsgiverNotifikasjonKlient: ArbeidsgiverNotifikasjonKlient
) : BakgrunnsjobbProsesserer {
    private val logger = this.logger()

    companion object {
        const val JOB_TYPE = "arbeidsgivernotifikasjon"
    }

    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        logger.info("Prosesserer ${jobb.uuid} med type ${jobb.type}")
        val jobbData = om.readValue<JobbData>(jobb.data)
        val sak = map(jobbData)
        val resultat = runBlocking {
            arbeidsgiverNotifikasjonKlient.opprettNySak(
                grupperingsid = sak.id.toString(),
                merkelapp = "Fritak arbeidsgiverperiode",
                virksomhetsnummer = sak.virkomhetsnummer,
                tittel = sak.tittel,
                lenke = sak.lenke,
                harddeleteOm = sak.harddeleteOm,
                statusTekst = "status"
            )
        }
        updateSaksId(jobbData, resultat)
        logger.info("Opprettet sak i arbeidsgivernotifikasjon med ${sak.id} med ref $resultat")
    }

    private fun genererTittel(navn: String?, identitetsnummer: String) =
        "Fritak fra arbeidsgiverperioden: $navn - f. ${identitetsnummer.take(6)}"

    private fun updateSaksId(jobbData: JobbData, id: String) {
        if (jobbData.skjemaType == JobbData.SkjemaType.KroniskKrav) {
            val skjema = kroniskKravRepo.getById(jobbData.skjemaId)
                ?: throw IllegalArgumentException("Fant ikke $jobbData")
            skjema.arbeidsgiverSakId = id
            kroniskKravRepo.update(skjema)
        } else {
            val skjema = gravidKravRepo.getById(jobbData.skjemaId)
                ?: throw IllegalArgumentException("Fant ikke $jobbData")
            skjema.arbeidsgiverSakId = id
            gravidKravRepo.update(skjema)
        }
    }

    private fun map(jobbData: JobbData): SakParametere {
        if (jobbData.skjemaType == JobbData.SkjemaType.KroniskKrav) {
            val skjema = kroniskKravRepo.getById(jobbData.skjemaId)
                ?: throw IllegalArgumentException("Fant ikke $jobbData")
            return SakParametere(
                skjema.id,
                skjema.virksomhetsnummer,
                genererTittel(skjema.navn, skjema.identitetsnummer),
                "$frontendAppBaseUrl/nb/kronisk/krav/${skjema.id}",
                "P3Y"
            )
        } else {
            val skjema = gravidKravRepo.getById(jobbData.skjemaId)
                ?: throw IllegalArgumentException("Fant ikke $jobbData")
            return SakParametere(
                skjema.id,
                skjema.virksomhetsnummer,
                genererTittel(skjema.navn, skjema.identitetsnummer),
                "$frontendAppBaseUrl/nb/gravid/krav/${skjema.id}",
                "P1Y"
            )
        }
    }

    data class SakParametere(
        val id: UUID,
        val virkomhetsnummer: String,
        val tittel: String,
        val lenke: String,
        val harddeleteOm: String
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
