package no.nav.helse.fritakagp.processing.gravid.soeknad

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.domain.sladdFnr
import no.nav.helse.fritakagp.domain.TIMESTAMP_FORMAT_MED_KL
import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.domain.Tiltak

interface GravidSoeknadKvitteringSender {
    fun send(kvittering: GravidSoeknad)
}

class GravidSoeknadKvitteringSenderDummy : GravidSoeknadKvitteringSender {
    override fun send(kvittering: GravidSoeknad) {
        println("Sender kvittering for søknad gravid: ${kvittering.id}")
    }
}

class GravidSoeknadAltinnKvitteringSender(
    private val altinnTjenesteKode: String,
    private val iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
    private val username: String,
    private val password: String
) : GravidSoeknadKvitteringSender {

    companion object {
        const val SYSTEM_USER_CODE = "NAV_HELSEARBEIDSGIVER"
    }

    override fun send(kvittering: GravidSoeknad) {
        try {
            val receiptExternal = iCorrespondenceAgencyExternalBasic.insertCorrespondenceBasicV2(
                username,
                password,
                SYSTEM_USER_CODE,
                kvittering.id.toString(),
                mapKvitteringTilInsertCorrespondence(kvittering)
            )
            if (receiptExternal.receiptStatusCode != ReceiptStatusEnum.OK) {
                throw RuntimeException("Fikk uventet statuskode fra Altinn: ${receiptExternal.receiptStatusCode} ${receiptExternal.receiptText}")
            }
        } catch (e: ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage) {
            throw RuntimeException("Feil fra altinn: ${e.faultInfo}", e)
        }
    }

    fun mapKvitteringTilInsertCorrespondence(kvittering: GravidSoeknad): InsertCorrespondenceV2 {
        val sladdetFnr = sladdFnr(kvittering.identitetsnummer)
        val tittel = "$sladdetFnr - Kvittering for mottatt søknad om fritak fra arbeidsgiverperioden grunnet graviditet"

        val innhold = """
        <html>
           <head>
               <meta charset="UTF-8">
           </head>
           <body>
               <div class="melding">
            <p>Kvittering for mottatt søknad om fritak fra arbeidsgiverperioden grunnet risiko for høyt sykefravær knyttet til graviditet.</p>
            <p>Virksomhetsnummer: ${kvittering.virksomhetsnummer}</p>
            <p>${kvittering.opprettet.format(TIMESTAMP_FORMAT_MED_KL)}</p>
            <p>Søknaden vil bli behandlet fortløpende. Ved behov vil NAV innhente ytterligere dokumentasjon.
             Har dere spørsmål, ring NAVs arbeidsgivertelefon 55 55 33 36.</p>
            <p>Dere har innrapportert følgende:</p>
            <ul>
                <li>Navn: ${kvittering.navn}</li>
                <li>Dokumentasjon vedlagt: ${jaEllerNei(kvittering.harVedlegg)}</li>
                <li>Forsøkt tilrettelegging: ${jaEllerNei(kvittering.tilrettelegge)}</li>
                <li>Tiltak:${lagreTiltak(kvittering.tiltak)} </li>
                <li>Forsøkt omplassering: ${lagreOmplasseringStr(kvittering)}</li>
                <li>Mottatt: ${kvittering.opprettet.format(TIMESTAMP_FORMAT_MED_KL)}</li>
                <li>Innrapportert av: ${kvittering.sendtAvNavn}</li>
            </ul>
               </div>
           </body>
        </html>
        """.trimIndent()

        val meldingsInnhold = ExternalContentV2()
            .withLanguageCode("1044")
            .withMessageTitle(tittel)
            .withMessageBody(innhold)
            .withMessageSummary("Kvittering for søknad om fritak fra arbeidsgiverperioden ifbm graviditetsrelatert fravær")

        return InsertCorrespondenceV2()
            .withAllowForwarding(false)
            .withReportee(kvittering.virksomhetsnummer)
            .withMessageSender("NAV (Arbeids- og velferdsetaten)")
            .withServiceCode(altinnTjenesteKode)
            .withServiceEdition("1")
            .withContent(meldingsInnhold)
    }

    fun jaEllerNei(verdi: Boolean) = if (verdi) "Ja" else "Nei"
    fun lagreOmplasseringStr(kvittering: GravidSoeknad): String {
        return if (kvittering.omplassering != null) {
            if (kvittering.omplassering == Omplassering.IKKE_MULIG) {
                """
                    ${kvittering.omplassering.beskrivelse} fordi  ${kvittering.omplasseringAarsak?.beskrivelse}
                """
            } else {
                kvittering.omplassering.beskrivelse
            }
        } else {
            ""
        }
    }

    fun lagreTiltak(tiltak: List<Tiltak>?): String {
        var ret = ""
        if (tiltak != null) {
            ret += "<ul>"
            for (t in tiltak)
                ret += "<li>${t.beskrivelse}</li>"
            ret += "</ul>"
        } else {
            ret = "Ingen tiltak"
        }

        return ret
    }
}
