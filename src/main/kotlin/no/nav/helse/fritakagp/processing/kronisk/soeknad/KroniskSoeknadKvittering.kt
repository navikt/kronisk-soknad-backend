package no.nav.helse.fritakagp.processing.kronisk.soeknad

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import java.time.format.DateTimeFormatter

interface KroniskSoeknadKvitteringSender {
    fun send(kvittering: KroniskSoeknad)
}

class KroniskSoeknadKvitteringSenderDummy: KroniskSoeknadKvitteringSender {
    override fun send(kvittering: KroniskSoeknad) {
        println("Sender kvittering for søknad gravid: ${kvittering.id}")
    }
}

class KroniskSoeknadAltinnKvitteringSender(
    private val altinnTjenesteKode: String,
    private val iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
    private val username: String,
    private val password: String) : KroniskSoeknadKvitteringSender {

    companion object {
        const val SYSTEM_USER_CODE = "NAV_HELSEARBEIDSGIVER"
    }

    override fun send(kvittering: KroniskSoeknad) {
        try {
            val receiptExternal = iCorrespondenceAgencyExternalBasic.insertCorrespondenceBasicV2(
                    username, password,
                    SYSTEM_USER_CODE, kvittering.id.toString(),
                    mapKvitteringTilInsertCorrespondence(kvittering)
            )
            if (receiptExternal.receiptStatusCode != ReceiptStatusEnum.OK) {
                throw RuntimeException("Fikk uventet statuskode fra Altinn: ${receiptExternal.receiptStatusCode} ${receiptExternal.receiptText}")
            }
        } catch (e: ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage) {
            throw RuntimeException("Feil fra altinn: ${e.faultInfo}", e)
        }
    }

    fun mapKvitteringTilInsertCorrespondence(kvittering: KroniskSoeknad): InsertCorrespondenceV2 {
        val dateTimeFormatterMedKl = DateTimeFormatter.ofPattern("dd.MM.yyyy 'kl.' HH:mm")
        val tittel = "Kvittering for mottatt søknad om fritak fra arbeidsgiverperioden grunnet kronisk sykdom"

        val innhold = """
        <html>
           <head>
               <meta charset="UTF-8">
           </head>
           <body>
               <div class="melding">
            <p>Kvittering for mottatt søknad om fritak fra arbeidsgiverperioden grunnet risiko for høyt sykefravær knyttet til kronisk sykdom.</p>
            <p>Virksomhetsnummer: ${kvittering.virksomhetsnummer}</p>
            <p>${kvittering.opprettet.format(dateTimeFormatterMedKl)}/p>
            <p>Søknaden vil bli behandlet fortløpende. Ved behov vil NAV innhente ytterligere dokumentasjon.
             Har dere spørsmål, ring NAVs arbeidsgivertelefon 55 55 33 36.</p>
            <p>Dere har innrapportert følgende:</p>
            <ul>
                <li>Fødselsnummer: xxxxxxxxxxx
                <li>Forsøkt tilrettelegging [Ja/Nei]
                <li>Tiltak: [Liste over tiltak]
                <li>Forsøkt omplassering: [Ja/Nei/Ikke mulig + grunn]
                <li>Dokumentasjon vedlagt: [Ja/Nei]
                <li>>Mottatt: dd.mm.åååå kl tt:mm</li>
                <li>Innrapportert av [fnr på innsender]</li>
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
}