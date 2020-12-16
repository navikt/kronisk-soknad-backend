package no.nav.helse.fritakagp.processing.kvittering

import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import java.time.format.DateTimeFormatter

class AltinnKvitteringMapper(val altinnTjenesteKode: String) {


    fun mapKvitteringTilInsertCorrespondence(kvittering: Kvittering): InsertCorrespondenceV2 {
        val dateTimeFormatterMedKl = DateTimeFormatter.ofPattern("dd.MM.yyyy 'kl.' HH:mm")
        val tittel = "Kvittering for mottatt søknad om fritak fra arbeidsgiverperioden grunnet graviditet"

        val innhold = """
        <html>
           <head>
               <meta charset="UTF-8">
           </head>
           <body>
               <div class="melding">
<p>Kvittering for mottatt søknad om fritak fra arbeidsgiverperioden grunnet risiko for høyt sykefravær knyttet til graviditet.</p>
<p>Virksomhetsnummer: ${kvittering.virksomhetsnummer}</p>
<p>${kvittering.tidspunkt.format(dateTimeFormatterMedKl)}/p>
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
                .withMessageSummary("Kvittering for krav om utvidet refusjon ved koronaviruset")


        return InsertCorrespondenceV2()
                .withAllowForwarding(false)
                .withReportee(kvittering.virksomhetsnummer)
                .withMessageSender("NAV (Arbeids- og velferdsetaten)")
                .withServiceCode(altinnTjenesteKode)
                .withServiceEdition("1")
                .withContent(meldingsInnhold)
    }
}
