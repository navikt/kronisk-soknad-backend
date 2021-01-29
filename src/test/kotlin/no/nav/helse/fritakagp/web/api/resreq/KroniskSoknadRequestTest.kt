package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.KroniskTestData
import no.nav.helse.fritakagp.domain.FravaerData
import no.nav.helse.fritakagp.domain.PaakjenningsType
import no.nav.helse.toYearMonthString
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class KroniskSoknadRequestTest {

    @Test
    fun `Gyldig FNR er påkrevd`() {
        validationShouldFailFor(KroniskSoknadRequest::identitetsnummer) {
            KroniskTestData.fullValidRequest.copy(identitetsnummer = "01020312345").validate()
        }
    }

    @Test
    fun `Gyldig OrgNr er påkrevd dersom det er oppgitt`() {
        validationShouldFailFor(KroniskSoknadRequest::virksomhetsnummer) {
            KroniskTestData.fullValidRequest.copy(virksomhetsnummer = "098765432").validate()
        }
    }

    @Test
    fun `Bekreftelse av egenerklæring er påkrevd`() {
        validationShouldFailFor(KroniskSoknadRequest::bekreftet) {
            KroniskTestData.fullValidRequest.copy(bekreftet = false).validate()
        }
    }

    @Test
    fun `Kan ikke ha eldre fraværsdata enn 3 år`() {
        KroniskTestData.fullValidRequest.copy(
            fravaer = setOf(
                FravaerData(LocalDate.now().minusMonths(36).toYearMonthString(), 5)
            )
        ).validate()

        validationShouldFailFor(KroniskSoknadRequest::fravaer) {
            KroniskTestData.fullValidRequest.copy(
                fravaer = setOf(
                    FravaerData(LocalDate.now().minusMonths(37).toYearMonthString(), 5)
                )
            ).validate()
        }
    }


    @Test
    fun `Om påkjenninger ikke inneholder "ANNET" er beskrivelse ikke påkrevd`() {
        KroniskTestData.fullValidRequest.copy(
            paakjenningstyper = setOf(PaakjenningsType.STRESSENDE),
            paakjenningBeskrivelse = null
        ).validate()
    }
    @Test
    fun `Om påkjenninger inneholder "ANNET" er beskrivelse påkrevd`() {
        validationShouldFailFor(KroniskSoknadRequest::paakjenningBeskrivelse) {
            KroniskTestData.fullValidRequest.copy(paakjenningBeskrivelse = null).validate()
        }
    }

    @Test
    fun `Kan ikke ha fraværsdata fra fremtiden`() {
        validationShouldFailFor(KroniskSoknadRequest::fravaer) {
            KroniskTestData.fullValidRequest.copy(
                fravaer = setOf(
                    FravaerData(LocalDate.now().plusMonths(1).toYearMonthString(), 5)
                )
            ).validate()
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
            ).validate()
        }
    }
}