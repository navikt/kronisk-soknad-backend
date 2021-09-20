package no.nav.helse.fritakagp.processing.kronisk.krav

import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.processing.gravid.krav.getPDFTimeStampFormat
import org.apache.commons.lang3.text.WordUtils
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType0Font
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter

class KroniskKravPDFGenerator {
    private val FONT_SIZE = 11f
    private val LINE_HEIGHT = 15f
    private val MARGIN_X = 40f
    private val MARGIN_Y = 40f
    private val FONT_NAME = "fonts/ARIALUNI.TTF"

    fun lagPDF(krav: KroniskKrav): ByteArray {
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
        content.showText("Krav om refusjon av sykepenger i arbeidsgiverperioden")
        content.setFont(font, FONT_SIZE)

        content.writeTextWrapped("Mottatt: ${getPDFTimeStampFormat().format(krav.opprettet)}", 4)
        content.writeTextWrapped("Person (FNR): ${krav.identitetsnummer}")
        content.writeTextWrapped("Arbeidsgiver oppgitt i krav: ${krav.virksomhetsnavn} (${krav.virksomhetsnummer})")
        content.writeTextWrapped("Perioder", 2)

        krav.perioder.forEach {
            val gradering = (it.gradering * 100).toString()
            with(content) {
                writeTextWrapped("FOM: ${it.fom}")
                writeTextWrapped("TOM: ${it.tom}")
                writeTextWrapped("Sykmeldingsgrad: ${gradering}%")
                writeTextWrapped("Antall dager det kreves refusjon for: ${it.antallDagerMedRefusjon}")
                writeTextWrapped("Beregnet månedsinntekt (NOK): ${it.månedsinntekt}")
                writeTextWrapped("Dagsats (NOK): ${it.dagsats}")
                writeTextWrapped("Beløp (NOK): ${it.belop}")
                writeTextWrapped("")
            }
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
        WordUtils.wrap(text, 120).split('\n').forEach {
            this.newLineAtOffset(0F, -LINE_HEIGHT * spacing)
            this.showText(it)
        }
    }
}
