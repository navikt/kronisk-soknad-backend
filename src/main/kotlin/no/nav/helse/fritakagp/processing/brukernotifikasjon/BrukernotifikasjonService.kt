package no.nav.helse.fritakagp.processing.brukernotifikasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import java.time.ZonedDateTime

class BrukernotifikasjonService(
    private val gravidKravRepo: GravidKravRepository,
    private val gravidSoeknadRepo: GravidSoeknadRepository,
    private val kroniskKravRepo: KroniskKravRepository,
    private val kroniskSoeknadRepo: KroniskSoeknadRepository,
    private val om: ObjectMapper,
    private val sensitivitetNivaa: Sensitivitet = Sensitivitet.High,
    private val frontendAppBaseUrl: String = "https://arbeidsgiver.nav.no/fritak-agp"
) {

    fun opprettVarsel(varselId: String, jobb: Bakgrunnsjobb): String {
        val jobbData = om.readValue<BrukernotifikasjonJobbdata>(jobb.data)

        return when (jobbData.skjemaType) {
            BrukernotifikasjonJobbdata.SkjemaType.KroniskKrav -> {
                val skjema = kroniskKravRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                getVarsel(
                    varselId = varselId,
                    identitetsnummer = skjema.identitetsnummer,
                    virksomhetsnavn = skjema.virksomhetsnavn,
                    jobbData = jobbData
                )
            }

            BrukernotifikasjonJobbdata.SkjemaType.KroniskSøknad -> {
                val skjema = kroniskSoeknadRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                getVarsel(
                    varselId = varselId,
                    identitetsnummer = skjema.identitetsnummer,
                    virksomhetsnavn = skjema.virksomhetsnavn,
                    jobbData = jobbData
                )
            }

            BrukernotifikasjonJobbdata.SkjemaType.GravidKrav -> {
                val skjema = gravidKravRepo.getById(jobbData.skjemaId) ?: throw IllegalArgumentException("Fant ikke $jobbData")
                getVarsel(
                    varselId = varselId,
                    identitetsnummer = skjema.identitetsnummer,
                    virksomhetsnavn = skjema.virksomhetsnavn,
                    jobbData = jobbData
                )
            }

            BrukernotifikasjonJobbdata.SkjemaType.GravidSøknad -> {
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

    private fun getVarsel(varselId: String, identitetsnummer: String, virksomhetsnavn: String?, jobbData: BrukernotifikasjonJobbdata) =
        VarselActionBuilder.opprett {
            type = Varseltype.Beskjed
            this.varselId = varselId
            sensitivitet = sensitivitetNivaa
            ident = identitetsnummer
            tekst = Tekst(
                spraakkode = "nb",
                tekst = jobbData.getTekst(virksomhetsnavn),
                default = true
            )
            link = frontendAppBaseUrl + jobbData.getLenke()
            aktivFremTil = ZonedDateTime.now().plusDays(31)
        }
}
