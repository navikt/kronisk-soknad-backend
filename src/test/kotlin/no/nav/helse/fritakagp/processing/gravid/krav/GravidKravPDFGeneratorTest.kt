package no.nav.helse.fritakagp.processing.gravid.krav

import no.nav.helse.GravidTestData
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.awt.Desktop
import java.nio.file.Files
import java.time.LocalDateTime

class GravidKravPDFGeneratorTest {

    @Test
    fun testLagPDF() {
        val krav = GravidTestData.gravidKrav
        val pdf = GravidKravPDFGenerator().lagPDF(krav)
        assertThat(pdf).isNotNull

        val pdfText = extractTextFromPdf(pdf)

        assertThat(pdfText).contains(krav.navn)
        assertThat(pdfText).contains(krav.virksomhetsnummer)
    }

    @Test
    @Disabled
    fun testLagSlettingPDF() {
        val krav = GravidTestData.gravidKrav
        krav.journalpostId = "12345"
        krav.endretDato = LocalDateTime.now()
        val pdf = GravidKravPDFGenerator().lagSlettingPDF(krav)
        assertThat(pdf).isNotNull

        val pdfText = extractTextFromPdf(pdf)

        assertThat(pdfText).contains(krav.navn)
        assertThat(pdfText).contains(krav.virksomhetsnummer)
        assertThat(pdfText).contains(krav.journalpostId)
    }

    @Test
    @Disabled
    fun saveAndShowPdf() {
        // test for å visuelt sjekke ut PDFen
        val krav = GravidTestData.gravidKrav
        val pdf = GravidKravPDFGenerator().lagPDF(krav)

        val file = Files.createTempFile(null, ".pdf").toFile()
        file.writeBytes(pdf)

        Desktop.getDesktop().open(file)
    }

    private fun extractTextFromPdf(pdf: ByteArray): String? {
        val pdfReader = PDDocument.load(pdf)
        val pdfStripper = PDFTextStripper()
        val allTextInDocument = pdfStripper.getText(pdfReader)
        pdfReader.close()
        return allTextInDocument
    }
}
