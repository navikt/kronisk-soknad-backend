package no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.arbeidsgiver.integrasjoner.arbeidsgiverNotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.helsearbeidsgiver.graphql.generated.ISO8601DateTime
import org.joda.time.LocalDateTime
import org.slf4j.LoggerFactory
import java.util.*

class ArbeidsgiverNotifikasjonProcessor(
    private val gravidKravRepo: GravidKravRepository,
    private val kroniskKravRepo: KroniskKravRepository,
    private val om: ObjectMapper,
    private val frontendAppBaseUrl: String = "https://arbeidsgiver.nav.no/fritak-agp",
    private val arbeidsgiverNotifikasjonKlient: ArbeidsgiverNotifikasjonKlient
) : BakgrunnsjobbProsesserer {
    val log = LoggerFactory.getLogger(ArbeidsgiverNotifikasjonProcessor::class.java)

    companion object {
        const val JOB_TYPE = "arbeidsgivernotifikasjon"
    }

    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        log.info("Prosesserer ${jobb.uuid} med type ${jobb.type}")
        val jobbData = om.readValue<Jobbdata>(jobb.data)
        val sak = map(jobbData)
        val resultat = arbeidsgiverNotifikasjonKlient.opprettNySak(
            grupperingsid = sak.id.toString(),
            merkelapp = "Refusjon",
            virksomhetsnummer = sak.virkomhetsnummer,
            tittel = sak.tittel,
            lenke = sak.lenke,
            tidspunkt = LocalDateTime.now().toString()
        )
        log.info("Opprettet sak i arbeidsgivernotifikasjon med ${sak.id} med ref $resultat")
    }

    private fun map(jobbData: Jobbdata): SakParametere {
        if (jobbData.skjemaType == Jobbdata.SkjemaType.KroniskKrav) {
            val skjema = kroniskKravRepo.getById(jobbData.skjemaId)
                ?: throw IllegalArgumentException("Fant ikke $jobbData")
            return SakParametere(
                skjema.id,
                skjema.virksomhetsnummer,
                "${skjema.navn} - ${skjema.identitetsnummer.substring(0, 6)} Refusjon arbeidsgiverperiode - kronisk sykdom",
                "$frontendAppBaseUrl/nb/kronisk/krav/${skjema.id}",
                skjema.opprettet.toString()
            )
        }

        val skjema = gravidKravRepo.getById(jobbData.skjemaId)
            ?: throw IllegalArgumentException("Fant ikke $jobbData")
        return SakParametere(
            skjema.id,
            skjema.virksomhetsnummer,
            "${skjema.navn} - ${skjema.identitetsnummer.substring(0, 6)} Refusjon arbeidsgiverperiode - graviditet",
            "$frontendAppBaseUrl/nb/gravid/krav/${skjema.id}",
            skjema.opprettet.toString()
        )
    }

    data class SakParametere(
        val id: UUID,
        val virkomhetsnummer: String,
        val tittel: String,
        val lenke: String,
        val tidspunkt: ISO8601DateTime?
    )

    data class Jobbdata(
        val skjemaId: UUID,
        val skjemaType: SkjemaType
    ) {
        enum class SkjemaType {
            KroniskKrav,
            GravidKrav
        }
    }
}
