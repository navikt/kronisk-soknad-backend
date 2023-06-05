package no.nav.helse.fritakagp.processing.gravid.krav

import no.nav.helse.AgpTestData
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.domain.ArbeidsgiverperiodeNy
import no.nav.helse.fritakagp.domain.Periode
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.awt.Desktop
import java.nio.file.Files
import java.time.LocalDateTime
import kotlin.math.roundToInt
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

class GravidKravPDFGeneratorTest {

    @Test
    fun testLagPDF() {
        val periode = Periode(
            fom = LocalDate.of(2022, 4, 1),
            tom = LocalDate.of(2022, 4, 16)
        )

        val krav = GravidTestData.gravidKrav.copy(
            perioder = listOf(
                ArbeidsgiverperiodeNy(
                    perioder = listOf(
                        periode.copy(
                            fom = LocalDate.of(2022, 4, 1),
                            tom = LocalDate.of(2022, 4, 7)
                        ),
                        periode.copy(
                            fom = LocalDate.of(2022, 4, 8),
                            tom = LocalDate.of(2022, 4, 14)
                        ),
                        periode.copy(
                            fom = LocalDate.of(2022, 4, 14),
                            tom = LocalDate.of(2022, 4, 16)
                        )
                    ),
                    antallDagerMedRefusjon = 3,
                    månedsinntekt = 3000.0
                )
            )
        )
        val pdf = GravidKravPDFGenerator().lagPDF(krav)
        assertThat(pdf).isNotNull

        val pdfText = extractTextFromPdf(pdf)
        val antallSider = numberOfPagesInPDF(pdf)

        assertThat(pdfText).contains(krav.navn)
        assertThat(pdfText).contains(krav.virksomhetsnummer)
        assertThat(pdfText).contains(krav.perioder.first().månedsinntekt.roundToInt().toString())
        assertThat(antallSider).isEqualTo(1)

        //TODO fjern unødvendige linjer for å generer pdf fil
        //val file = File("src/test/resources/testfil.pdf")
        //val writer = FileOutputStream(file)
        //writer.write(pdf)
    }

    @Test
    fun `test lag krav over flere sider`() {
        val krav = GravidTestData.gravidLangtKrav
        val pdf = GravidKravPDFGenerator().lagPDF(krav)
        assertThat(pdf).isNotNull

        val antallSider = numberOfPagesInPDF(pdf)

        assertThat(antallSider).isEqualTo(2)
    }

    @Test
    fun testLagSlettingPDF() {
        val krav = GravidTestData.gravidKrav.copy(journalpostId = "12345", endretDato = LocalDateTime.now())
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

    private fun numberOfPagesInPDF(pdf: ByteArray): Int {
        val pdfReader = PDDocument.load(pdf)
        return pdfReader.numberOfPages
    }

    private fun extractTextFromPdf(pdf: ByteArray): String? {
        val pdfReader = PDDocument.load(pdf)
        val pdfStripper = PDFTextStripper()
        val allTextInDocument = pdfStripper.getText(pdfReader)
        pdfReader.close()
        return allTextInDocument
    }
}
