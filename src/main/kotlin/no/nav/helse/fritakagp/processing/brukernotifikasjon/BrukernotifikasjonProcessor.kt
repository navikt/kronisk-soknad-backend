package no.nav.helse.fritakagp.processing.brukernotifikasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.BrukernotifikasjonerMetrics
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helse.fritakagp.integration.kafka.BrukernotifikasjonBeskjedSender
import no.nav.helsearbeidsgiver.utils.log.logger
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class BrukernotifikasjonProcessor(
    private val gravidKravRepo: GravidKravRepository,
    private val gravidSoeknadRepo: GravidSoeknadRepository,
    private val kroniskKravRepo: KroniskKravRepository,
    private val kroniskSoeknadRepo: KroniskSoeknadRepository,
    private val om: ObjectMapper,
    private val kafkaProducerFactory: BrukernotifikasjonBeskjedSender,
    private val sikkerhetsNivaa: Int = 4,
    private val frontendAppBaseUrl: String = "https://arbeidsgiver.nav.no/fritak-agp"
) : BakgrunnsjobbProsesserer {

    private val logger = this.logger()

    companion object {
        val JOB_TYPE = "brukernotifikasjon"
    }

    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        logger.info("Prosesserer ${jobb.uuid} med type ${jobb.type}")
        val jobbData = om.readValue<Jobbdata>(jobb.data)
        val beskjed = map(jobbData)
        val uuid = UUID.randomUUID().toString()

        val nokkel = NokkelInputBuilder()
            .withEventId(uuid)
            .withFodselsnummer(beskjed.fnr)
            .withGrupperingsId(beskjed.skjemaId)
            .withNamespace("helsearbeidsgiver")
            .withAppnavn("fritakagp")
            .build()
        kafkaProducerFactory.sendMessage(nokkel, beskjed.beskjed)
        BrukernotifikasjonerMetrics.labels(jobbData.skjemaType.name).inc()
    }

    private fun map(jobbData: Jobbdata): BeskjedMedFnr {
        return when (jobbData.skjemaType) {
            Jobbdata.SkjemaType.KroniskKrav -> {
                val skjema = kroniskKravRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                BeskjedMedFnr(
                    fnr = skjema.identitetsnummer,
                    skjemaId = skjema.id.toString(),
                    beskjed = buildBeskjed(
                        linkUrl = "$frontendAppBaseUrl/nb/notifikasjon/kronisk/krav/${skjema.id}",
                        hendselstidspunkt = skjema.opprettet,
                        virksomhetsNavn = skjema.virksomhetsnavn
                    )
                )
            }

            Jobbdata.SkjemaType.KroniskSøknad -> {
                val skjema = kroniskSoeknadRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                BeskjedMedFnr(skjema.identitetsnummer, skjema.id.toString(), buildBeskjed("$frontendAppBaseUrl/nb/notifikasjon/kronisk/soknad/${skjema.id}", skjema.opprettet, skjema.virksomhetsnavn))
            }

            Jobbdata.SkjemaType.GravidKrav -> {
                val skjema = gravidKravRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                BeskjedMedFnr(skjema.identitetsnummer, skjema.id.toString(), buildBeskjed("$frontendAppBaseUrl/nb/notifikasjon/gravid/krav/${skjema.id}", skjema.opprettet, skjema.virksomhetsnavn))
            }

            Jobbdata.SkjemaType.GravidSøknad -> {
                val skjema = gravidSoeknadRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                BeskjedMedFnr(skjema.identitetsnummer, skjema.id.toString(), buildBeskjed("$frontendAppBaseUrl/nb/notifikasjon/gravid/soknad/${skjema.id}", skjema.opprettet, skjema.virksomhetsnavn))
            }
        }
    }

    private fun buildBeskjed(
        linkUrl: String,
        hendselstidspunkt: LocalDateTime,
        virksomhetsNavn: String?
    ): BeskjedInput {
        val synligFremTil = LocalDateTime.now().plusDays(31)
        val ukjentArbeidsgiver = "Arbeidsgiveren din"
        val hendelsestidsPunktUtc = hendselstidspunkt
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(ZoneId.of("UTC"))
            .toLocalDateTime()

        val beskjed = BeskjedInputBuilder()
            .withLink(URL(linkUrl))
            .withSikkerhetsnivaa(sikkerhetsNivaa)
            .withSynligFremTil(synligFremTil)
            .withTekst("${virksomhetsNavn ?: ukjentArbeidsgiver} har søkt om utvidet støtte fra NAV angående sykepenger til deg.old")
            .withEksternVarsling(true)
            .withTidspunkt(hendelsestidsPunktUtc)
            .build()

        return beskjed
    }

    data class Jobbdata(
        val skjemaId: UUID,
        val skjemaType: SkjemaType
    ) {
        enum class SkjemaType {
            KroniskKrav,
            KroniskSøknad,
            GravidKrav,
            GravidSøknad
        }
    }
}

data class BeskjedMedFnr(
    val fnr: String,
    val skjemaId: String,
    val beskjed: BeskjedInput
)
