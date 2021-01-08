package no.nav.helse.fritakagp.processing.kronisk

import no.nav.helse.KroniskTestData
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.awt.Desktop
import java.nio.file.Files

class KroniskSoeknadPDFGeneratorTest {

    @Test
    fun testLagPDF() {
        val soeknad = KroniskTestData.soeknadKronisk
        val pdf = KroniskSoeknadPDFGenerator().lagPDF(soeknad)
        assertThat(pdf).isNotNull

        val pdfText = extractTextFromPdf(pdf)

        assertThat(pdfText).contains(soeknad.fnr)
        assertThat(pdfText).contains(soeknad.orgnr)

        soeknad.paakjenningstyper.forEach { assertThat((pdfText)?.contains(it.beskrivelse)) }
        assertThat(pdfText).contains(soeknad.paakjenningBeskrivelse)
        soeknad.arbeidstyper.forEach { assertThat((pdfText)?.contains(it.beskrivelse)) }

        soeknad.fravaer.forEach {
            assertThat(pdfText).contains(it.antallDagerMedFravaer.toString())
        }
    }

    @Test
    @Disabled
    fun saveAndShowPdf() {
        // test for å visuelt sjekke ut PDFen
        val soeknad = KroniskTestData.soeknadKronisk
        val pdf = KroniskSoeknadPDFGenerator().lagPDF(soeknad)

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