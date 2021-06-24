package no.nav.helse.fritakagp.processing.kronisk.soeknad

import no.nav.helse.fritakagp.domain.KroniskSoeknad
import no.nav.helse.fritakagp.processing.gravid.krav.getPDFTimeStampFormat
import org.apache.commons.lang3.text.WordUtils
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType0Font
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter

class KroniskSoeknadPDFGenerator {
    private val FONT_SIZE = 11f
    private val LINE_HEIGHT = 15f
    private val MARGIN_X = 40f
    private val MARGIN_Y = 40f
    private val FONT_NAME = "fonts/ARIALUNI.TTF"


    fun lagPDF(soeknad: KroniskSoeknad): ByteArray {
        val doc = PDDocument()
        val font = PDType0Font.load(doc, this::class.java.classLoader.getResource(FONT_NAME).openStream())

        var curPage = PDPage()
        doc.addPage(curPage)

        val content = PDPageContentStream(doc, curPage)
        content.beginText()
        val mediaBox = curPage.mediaBox
        val startX = mediaBox.lowerLeftX + MARGIN_X
        val startY = mediaBox.upperRightY - MARGIN_Y
        content.newLineAtOffset(startX, startY)
        content.setFont(font, FONT_SIZE + 4)
        content.showText("Søknad om sykepenger for kronisk sykdom")
        content.setFont(font, FONT_SIZE)

        content.writeTextWrapped("Mottatt: ${getPDFTimeStampFormat().format(soeknad.opprettet)}", 4)
        content.writeTextWrapped("Person (FNR): ${soeknad.identitetsnummer}")
        content.writeTextWrapped("Arbeidsgiver oppgitt i søknad: ${soeknad.virksomhetsnavn} (${soeknad.virksomhetsnummer})")
        content.writeTextWrapped("Hva slags arbeid utfører den ansatte?", 2)
        soeknad.arbeidstyper.forEach { content.writeTextWrapped(" - ${it.beskrivelse}") }
        content.writeTextWrapped("Hvilke påkjenninger innebærer arbeidet?", 2)
        soeknad.paakjenningstyper.forEach { content.writeTextWrapped(" - ${it.beskrivelse}") }
        soeknad.paakjenningBeskrivelse?.let { content.writeTextWrapped(it) }

        val totaltAntallDager = soeknad.fravaer.map { it.antallDagerMedFravaer }.sum()
        content.writeTextWrapped("Totalt antall fraværsdager siste 2 år: $totaltAntallDager", 2)

        content.writeTextWrapped("Fraværsdager per måned siste 2 år: ", 2)
        content.writeTextWrapped("Antall fraværsperioder siste 2 år: ${soeknad.antallPerioder}")
        val yearlyFravaer = soeknad.fravaer.sortedByDescending { it.yearMonth }.groupBy { it.yearMonth.substring(0,4) }

        yearlyFravaer.forEach { yearGroup ->
            content.writeTextWrapped(yearGroup.key)
            content.newLineAtOffset(0F, -LINE_HEIGHT)
            yearGroup.value.sortedBy { it.yearMonth }.forEach {
                content.showText("${it.toLocalDate().toName()}: ${it.antallDagerMedFravaer}   ")
            }
            content.newLineAtOffset(0F, -LINE_HEIGHT)
        }

        content.endText()
        content.close()
        val out = ByteArrayOutputStream()
        doc.save(out)
        val ba = out.toByteArray()
        doc.close()
        return ba
    }

    private fun LocalDate.toName(): String {
        return when(this.month) {
            Month.JANUARY -> "Jan"
            Month.FEBRUARY -> "Feb"
            Month.MARCH -> "Mar"
            Month.APRIL -> "Apr"
            Month.MAY -> "Mai"
            Month.JUNE -> "Jun"
            Month.JULY -> "Jul"
            Month.AUGUST -> "Aug"
            Month.SEPTEMBER -> "Sep"
            Month.OCTOBER -> "Okt"
            Month.NOVEMBER -> "Nov"
            Month.DECEMBER -> "Des"
        }
    }

    private fun PDPageContentStream.writeTextWrapped(text: String, spacing: Int = 1) {
        WordUtils.wrap(text, 120).split('\n').forEach {

            this.newLineAtOffset(0F, -LINE_HEIGHT * spacing)
            this.showText(it)
        }
    }
}
