package no.nav.helse.fritakagp.processing.brukernotifikasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helse.fritakagp.integration.kafka.BrukernotifikasjonSender
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor.Jobbdata.NotifikasjonType.Annullere
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor.Jobbdata.NotifikasjonType.Endre
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor.Jobbdata.NotifikasjonType.Opprette
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import java.time.ZonedDateTime
import java.util.UUID

class BrukernotifikasjonProcessor(
    private val gravidKravRepo: GravidKravRepository,
    private val gravidSoeknadRepo: GravidSoeknadRepository,
    private val kroniskKravRepo: KroniskKravRepository,
    private val kroniskSoeknadRepo: KroniskSoeknadRepository,
    private val om: ObjectMapper,
    private val brukerNotifikasjonProducerFactory: BrukernotifikasjonSender,
    private val sensitivitetNivaa: Sensitivitet = Sensitivitet.High,
    private val frontendAppBaseUrl: String = "https://arbeidsgiver.nav.no/fritak-agp"
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

        return when (jobbData.skjemaType) {
            Jobbdata.SkjemaType.KroniskKrav -> {
                val skjema = kroniskKravRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                getVarsel(
                    varselId = varselId,
                    identitetsnummer = skjema.identitetsnummer,
                    virksomhetsnavn = skjema.virksomhetsnavn,
                    jobbData = jobbData
                )
            }

            Jobbdata.SkjemaType.KroniskSøknad -> {
                val skjema = kroniskSoeknadRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                getVarsel(
                    varselId = varselId,
                    identitetsnummer = skjema.identitetsnummer,
                    virksomhetsnavn = skjema.virksomhetsnavn,
                    jobbData = jobbData
                )
            }

            Jobbdata.SkjemaType.GravidKrav -> {
                val skjema = gravidKravRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                getVarsel(
                    varselId = varselId,
                    identitetsnummer = skjema.identitetsnummer,
                    virksomhetsnavn = skjema.virksomhetsnavn,
                    jobbData = jobbData
                )
            }

            Jobbdata.SkjemaType.GravidSøknad -> {
                val skjema = gravidSoeknadRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                getVarsel(
                    varselId = varselId,
                    identitetsnummer = skjema.identitetsnummer,
                    virksomhetsnavn = skjema.virksomhetsnavn,
                    jobbData = jobbData
                )
            }
        }
    }

    private fun getVarsel(varselId: String, identitetsnummer: String, virksomhetsnavn: String?, jobbData: Jobbdata) =
        VarselActionBuilder.opprett {
            type = Varseltype.Beskjed
            this.varselId = varselId
            sensitivitet = sensitivitetNivaa
            ident = identitetsnummer
            tekst = Tekst(
                spraakkode = "nb",
                tekst = (virksomhetsnavn ?: ukjentArbeidsgiver) + " " + jobbData.getTekst(),
                default = true
            )
            link = frontendAppBaseUrl + jobbData.getLenke()
            aktivFremTil = ZonedDateTime.now().plusDays(31)
        }

    data class Jobbdata(
        val skjemaId: UUID,
        val skjemaType: SkjemaType,
        val notifikasjonType: NotifikasjonType
    ) {
        fun getTekst(): String {
            return when (notifikasjonType) {
                Opprette -> "har søkt om utvidet støtte fra NAV angående sykepenger til deg."
                Endre -> "Endret"
                Annullere -> "Annullert"
            }
        }
        fun getLenke(): String {
            when (skjemaType) {
                SkjemaType.KroniskKrav -> {
                    return when (notifikasjonType) {
                        Opprette -> "/nb/notifikasjon/kronisk/krav/$skjemaId"
                        Endre -> TODO()
                        Annullere -> TODO()
                    }
                }
                SkjemaType.KroniskSøknad -> {
                    return when (notifikasjonType) {
                        Opprette -> "/nb/notifikasjon/kronisk/soknad/$skjemaId"
                        Endre -> TODO()
                        Annullere -> TODO()
                    }
                }
                SkjemaType.GravidKrav -> {
                    return when (notifikasjonType) {
                        Opprette -> "/nb/notifikasjon/gravid/krav/$skjemaId"
                        Endre -> TODO()
                        Annullere -> TODO()
                    }
                }
                SkjemaType.GravidSøknad -> {
                    return when (notifikasjonType) {
                        Opprette -> "/nb/notifikasjon/gravid/soknad/$skjemaId"
                        Endre -> TODO()
                        Annullere -> TODO()
                    }
                }
            }
        }
        enum class SkjemaType {
            KroniskKrav,
            KroniskSøknad,
            GravidKrav,
            GravidSøknad
        }
        enum class NotifikasjonType {
            Opprette,
            Endre,
            Annullere
        }
    }
}
