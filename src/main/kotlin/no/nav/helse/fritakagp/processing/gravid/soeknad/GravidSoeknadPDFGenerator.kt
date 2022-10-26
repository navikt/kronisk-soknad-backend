package no.nav.helse.fritakagp.processing.gravid.soeknad

import no.nav.helse.fritakagp.domain.DATE_FORMAT
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.domain.TIMESTAMP_FORMAT
import org.apache.commons.lang3.text.WordUtils
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType0Font
import java.io.ByteArrayOutputStream

class GravidSoeknadPDFGenerator {
    private val FONT_SIZE = 11f
    private val LINE_HEIGHT = 15f
    private val MARGIN_X = 40f
    private val MARGIN_Y = 40f
    private val FONT_NAME = "fonts/ARIALUNI.TTF"
    private val terminaDatoIkkeOppgitt = "Ikke oppgitt"

    fun lagPDF(soeknad: GravidSoeknad): ByteArray {
        val doc = PDDocument()
        val page = PDPage()
        val font = PDType0Font.load(doc, this::class.java.classLoader.getResource(FONT_NAME).openStream())
        doc.addPage(page)
        val content = PDPageContentStream(doc, page)
        content.beginText()
        val mediaBox = page.mediaBox
        val startX = mediaBox.lowerLeftX + MARGIN_X
        val startY = mediaBox.upperRightY - MARGIN_Y
        content.newLineAtOffset(startX, startY)
        content.setFont(font, FONT_SIZE + 4)
        content.showText(GravidSoeknad.tittel)
        content.setFont(font, FONT_SIZE)

        content.writeTextWrapped("Mottatt: ${TIMESTAMP_FORMAT.format(soeknad.opprettet)}", 4)
        content.writeTextWrapped("Sendt av: ${soeknad.sendtAvNavn}")
        content.writeTextWrapped("Person navn: ${soeknad.navn}")
        content.writeTextWrapped("Termindato: ${soeknad.termindato?.format(DATE_FORMAT) ?: terminaDatoIkkeOppgitt}")
        content.writeTextWrapped("Arbeidsgiver oppgitt i søknad: ${soeknad.virksomhetsnavn} (${soeknad.virksomhetsnummer})")
        content.writeTextWrapped(
            "Har dere prøvd å tilrettelegge arbeidsdagen slik at den gravide kan jobbe til tross for helseplagene?",
            2
        )

        if (soeknad.tilrettelegge) {
            content.writeTextWrapped("Ja")
            content.writeTextWrapped("Hvilke tiltak har dere forsøkt eller vurdert for at den ansatte kan jobbe?", 2)
            (soeknad.tiltak ?: emptyList())
                .forEach {
                    content.writeTextWrapped(" - ${it.beskrivelse}")
                }
            soeknad.tiltakBeskrivelse?.let { content.writeTextWrapped(it) }

            content.writeTextWrapped("Har dere forsøkt omplassering til en annen jobb?", 2)
            content.writeTextWrapped(soeknad.omplassering?.beskrivelse ?: "")
            soeknad.omplasseringAarsak?.let { content.writeTextWrapped(it.beskrivelse) }
        } else {
            content.writeTextWrapped("Nei")
        }

        content.endText()
        content.close()
        val out = ByteArrayOutputStream()
        doc.save(out)
        val ba = out.toByteArray()
        doc.close()
        return ba
    }

    private fun PDPageContentStream.writeTextWrapped(text: String, spacing: Int = 1) {
        WordUtils.wrap(text.replace("\t", " "), 100).split('\n').forEach {
            this.newLineAtOffset(0F, -LINE_HEIGHT * spacing)
            this.showText(it)
        }
    }
}
