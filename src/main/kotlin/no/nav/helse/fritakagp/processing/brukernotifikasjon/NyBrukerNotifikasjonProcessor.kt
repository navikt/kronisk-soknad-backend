package no.nav.helse.fritakagp.processing.brukernotifikasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helse.fritakagp.integration.kafka.BrukernotifikasjonKafkaProducer
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.tms.varsel.action.EksternVarslingBestilling
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import java.time.ZonedDateTime
import java.util.UUID

class NyBrukerNotifikasjonProcessor(
    private val gravidKravRepo: GravidKravRepository,
    private val gravidSoeknadRepo: GravidSoeknadRepository,
    private val kroniskKravRepo: KroniskKravRepository,
    private val kroniskSoeknadRepo: KroniskSoeknadRepository,
    private val om: ObjectMapper,
    private val brukerNotifikasjonProducerFactory: BrukernotifikasjonKafkaProducer,
    private val sensitivitetNivaa: Sensitivitet = Sensitivitet.High,
    private val frontendAppBaseUrl: String = "https://arbeidsgiver.nav.no/fritak-agp/nb/notifikasjon"
) : BakgrunnsjobbProsesserer {
    override val type: String get() = JOB_TYPE
    private val logger = this.logger()
    val ukjentArbeidsgiver = "Arbeidsgiveren din"
    companion object {
        val JOB_TYPE = "brukernotifikasjon"
    }
    override fun prosesser(jobb: Bakgrunnsjobb) {
        logger.info("Prosesserer ${jobb.uuid} med type ${jobb.type}")

        val varselId = UUID.randomUUID().toString()
        val varsel = opprettVarsel(varselId = varselId, jobb = jobb)
        brukerNotifikasjonProducerFactory.sendMessage(varselId = varselId, varsel = varsel)
    }

    private fun opprettVarsel(varselId: String, jobb: Bakgrunnsjobb): String {
        val jobbData = om.readValue<Jobbdata>(jobb.data)

        when (jobbData.skjemaType) {
            Jobbdata.SkjemaType.KroniskKrav -> {
                val skjema = kroniskKravRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                return getVarsel(
                    varselId = varselId,
                    identitetsnummer = skjema.identitetsnummer,
                    virksomhetsnavn = skjema.virksomhetsnavn,
                    lenke = "$frontendAppBaseUrl/kronisk/krav/${skjema.id}"
                )
            }

            Jobbdata.SkjemaType.KroniskSøknad -> {
                val skjema = kroniskSoeknadRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                return getVarsel(
                    varselId = varselId,
                    identitetsnummer = skjema.identitetsnummer,
                    virksomhetsnavn = skjema.virksomhetsnavn,
                    lenke = "$frontendAppBaseUrl/kronisk/soknad/${skjema.id}"
                )
            }

            Jobbdata.SkjemaType.GravidKrav -> {
                val skjema = gravidKravRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                return getVarsel(
                    varselId = varselId,
                    identitetsnummer = skjema.identitetsnummer,
                    virksomhetsnavn = skjema.virksomhetsnavn,
                    lenke = "$frontendAppBaseUrl/gravid/krav/${skjema.id}"
                )
            }

            Jobbdata.SkjemaType.GravidSøknad -> {
                val skjema = gravidSoeknadRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                return getVarsel(
                    varselId = varselId,
                    identitetsnummer = skjema.identitetsnummer,
                    virksomhetsnavn = skjema.virksomhetsnavn,
                    lenke = "$frontendAppBaseUrl/gravid/soknad/${skjema.id}"
                )
            }
        }
    }

    private fun getVarsel(varselId: String, identitetsnummer: String, virksomhetsnavn: String?, lenke: String) = VarselActionBuilder.opprett {
        type = Varseltype.Beskjed
        this.varselId = varselId
        sensitivitet = sensitivitetNivaa
        ident = identitetsnummer
        tekst = Tekst(
            spraakkode = "nb",
            tekst = "${virksomhetsnavn ?: ukjentArbeidsgiver} ny har søkt om utvidet støtte fra NAV angående sykepenger til deg. Denne kommer fra ny topic",
            default = true
        )
        produsent = Produsent(
            cluster = "dev-gcp", // TODO finn ut hvordan vi kan bruke miljøvariabler
            appnavn = "fritakagp",
            namespace = "helsearbeidsgiver"

        )
        link = lenke
        aktivFremTil = ZonedDateTime.now().plusDays(31)
        eksternVarsling = EksternVarslingBestilling()
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
