package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.KroniskTestData
import no.nav.helse.fritakagp.domain.FravaerData
import no.nav.helse.fritakagp.domain.PaakjenningsType
import no.nav.helse.toYearMonthString
import org.assertj.core.api.Assertions
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
    internal fun `mapping til domenemodell tar med harVedleggflagg`() {
        Assertions.assertThat(KroniskTestData.kroniskSoknadMedFil.toDomain("123").harVedlegg).isTrue()
        Assertions.assertThat(KroniskTestData.fullValidRequest.toDomain("123").harVedlegg).isFalse()
    }


    @Test
    fun `Kan ikke ha eldre fraværsdata enn 2 år`() {
        KroniskTestData.fullValidRequest.copy(
            fravaer = setOf(
                FravaerData(LocalDate.now().minusMonths(24).toYearMonthString(), 5)
            )
        ).validate()

        validationShouldFailFor(KroniskSoknadRequest::fravaer) {
            KroniskTestData.fullValidRequest.copy(
                fravaer = setOf(
                    FravaerData(LocalDate.now().minusMonths(25).toYearMonthString(), 5)
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