package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.GravidTestData
import no.nav.helse.KroniskTestData
import no.nav.helse.fritakagp.domain.FravaerData
import no.nav.helse.fritakagp.domain.PaakjenningsType
import no.nav.helse.fritakagp.domain.Tiltak
import no.nav.helse.toYearMonthString
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class KroniskSoknadRequestTest {

    @Test
    fun `Gyldig FNR er påkrevd`() {
        validationShouldFailFor(KroniskSoknadRequest::fnr) {
            KroniskTestData.fullValidRequest.copy(fnr = "01020312345")
        }
    }

    @Test
    fun `Gyldig OrgNr er påkrevd dersom det er oppgitt`() {
        validationShouldFailFor(KroniskSoknadRequest::orgnr) {
            KroniskTestData.fullValidRequest.copy(orgnr = "098765432")
        }
    }

    @Test
    fun `Bekreftelse av egenerklæring er påkrevd`() {
        validationShouldFailFor(KroniskSoknadRequest::bekreftet) {
            KroniskTestData.fullValidRequest.copy(bekreftet = false)
        }
    }

    @Test
    fun `Kan ikke ha eldre fraværsdata enn 3 år`() {
        KroniskTestData.fullValidRequest.copy(
            fravaer = setOf(
                FravaerData(LocalDate.now().minusMonths(36).toYearMonthString(), 5)
            )
        )

        validationShouldFailFor(KroniskSoknadRequest::fravaer) {
            KroniskTestData.fullValidRequest.copy(
                fravaer = setOf(
                    FravaerData(LocalDate.now().minusMonths(37).toYearMonthString(), 5)
                )
            )
        }
    }


    @Test
    fun `Om påkjenninger ikke inneholder "ANNET" er beskrivelse ikke påkrevd`() {
        KroniskTestData.fullValidRequest.copy(
            paakjenningstyper = setOf(PaakjenningsType.STRESSENDE),
            paakjenningBeskrivelse = null
        )
    }
    @Test
    fun `Om påkjenninger inneholder "ANNET" er beskrivelse påkrevd`() {
        validationShouldFailFor(KroniskSoknadRequest::paakjenningBeskrivelse) {
            KroniskTestData.fullValidRequest.copy(paakjenningBeskrivelse = null)
        }
    }

    @Test
    fun `Kan ikke ha fraværsdata fra fremtiden`() {
        validationShouldFailFor(KroniskSoknadRequest::fravaer) {
            KroniskTestData.fullValidRequest.copy(
                fravaer = setOf(
                    FravaerData(LocalDate.now().plusMonths(1).toYearMonthString(), 5)
                )
            )
        }
    }

    @Test
    fun `Kan ikke ha fraværsdager som overstiger antall dager i måneden`() {
        val invalidNumberOfDays = LocalDate.now().lengthOfMonth() + 1
        validationShouldFailFor(KroniskSoknadRequest::fravaer) {
            KroniskTestData.fullValidRequest.copy(
                fravaer = setOf(
                    FravaerData(LocalDate.now().toYearMonthString(), invalidNumberOfDays)
                )
            )
        }
    }
}