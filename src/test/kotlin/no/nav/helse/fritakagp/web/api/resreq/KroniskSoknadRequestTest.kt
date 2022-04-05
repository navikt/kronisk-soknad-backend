package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.KroniskTestData
import no.nav.helse.fritakagp.domain.FravaerData
import no.nav.helse.toYearMonthString
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class KroniskSoknadRequestTest {
    val navn = "Personliga Person"
    val sendtAv = "123"
    val sendtAvNavn = "Ola M Avsender"

    @Test
    fun `Gyldig FNR er påkrevd`() {
        validationShouldFailFor(KroniskSoknadRequest::identitetsnummer) {
            KroniskTestData.fullValidRequest.copy(identitetsnummer = "01020312345").validate(true)
        }
    }

    @Test
    fun `Gyldig OrgNr er påkrevd dersom det er oppgitt`() {
        validationShouldFailFor(KroniskSoknadRequest::virksomhetsnummer) {
            KroniskTestData.fullValidRequest.copy(virksomhetsnummer = "098765432").validate(true)
        }
    }

    @Test
    fun `Bekreftelse av egenerklæring er påkrevd`() {
        validationShouldFailFor(KroniskSoknadRequest::bekreftet) {
            KroniskTestData.fullValidRequest.copy(bekreftet = false).validate(true)
        }
    }

    @Test
    internal fun `mapping til domenemodell tar med harVedleggflagg`() {
        Assertions.assertThat(KroniskTestData.kroniskSoknadMedFil.toDomain(sendtAv, sendtAvNavn, navn).harVedlegg).isTrue()
        Assertions.assertThat(KroniskTestData.fullValidRequest.toDomain(sendtAv, sendtAvNavn, navn).harVedlegg).isFalse()
    }

    @Test
    fun `Kan ikke ha eldre fraværsdata enn 2 år`() {
        KroniskTestData.fullValidRequest.copy(
            fravaer = setOf(
                FravaerData(LocalDate.now().minusMonths(24).toYearMonthString(), 5)
            )
        ).validate(true)

        validationShouldFailFor(KroniskSoknadRequest::fravaer) {
            KroniskTestData.fullValidRequest.copy(
                fravaer = setOf(
                    FravaerData(LocalDate.now().minusMonths(25).toYearMonthString(), 5)
                )
            ).validate(true)
        }
    }

    @Test
    fun `Må være på virksomhetsnummer`() {
        validationShouldFailNTimesFor(KroniskSoknadRequest::virksomhetsnummer, 2) {
            KroniskTestData.fullValidRequest.copy().validate(false)
        }
    }

    @Test
    fun `Kan ikke ha fraværsdata fra fremtiden`() {
        validationShouldFailFor(KroniskSoknadRequest::fravaer) {
            KroniskTestData.fullValidRequest.copy(
                fravaer = setOf(
                    FravaerData(LocalDate.now().plusMonths(1).toYearMonthString(), 5)
                )
            ).validate(true)
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
            ).validate(true)
        }
    }
    @Test
    fun `Antall perioder kan ikke være 0`() {
        val invalidAntallPerioder = 0
        validationShouldFailFor(KroniskSoknadRequest::antallPerioder) {
            KroniskTestData.fullValidRequest.copy(
                antallPerioder = invalidAntallPerioder
            ).validate(true)
        }
    }
}
