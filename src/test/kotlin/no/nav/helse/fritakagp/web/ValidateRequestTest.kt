package no.nav.helse.fritakagp.web

import no.nav.helse.TestData
import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.domain.OmplasseringAarsak
import no.nav.helse.fritakagp.domain.Tiltak
import no.nav.helse.fritakagp.web.api.resreq.GravidSoknadRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.valiktor.ConstraintViolationException
import kotlin.reflect.KProperty1

class ValidateRequestTest {

    fun <B, A> validationShouldFailFor(field: KProperty1<B, A>, block: () -> Unit): Exception {
        val thrown = assertThrows<ConstraintViolationException>(block)
        assertThat(thrown.constraintViolations).hasSize(1)
        assertThat(thrown.constraintViolations.first().property).isEqualTo(field.name)
        return thrown
    }

    @Test
    internal fun `Gyldig FNR er påkrevd`() {
        validationShouldFailFor(GravidSoknadRequest::fnr) {
            TestData.fullValidRequest.copy(fnr = "01020312345")
        }
    }

    @Test
    internal fun `Gyldig OrgNr er påkrevd dersom det er oppgitt`() {
        validationShouldFailFor(GravidSoknadRequest::orgnr) {
            TestData.fullValidRequest.copy(orgnr = "098765432")
        }
    }

    @Test
    fun `Når tiltak inneholder ANNET må tiltaksbeskrivelse ha innhold`() {
        validationShouldFailFor(GravidSoknadRequest::tiltakBeskrivelse) {
            TestData.fullValidRequest.copy(
                tiltak = listOf(Tiltak.ANNET),
                tiltakBeskrivelse = "",
            )
        }

        TestData.fullValidRequest.copy(
            tiltak = listOf(Tiltak.ANNET),
            tiltakBeskrivelse = "dette går bra",
        )
    }

    @Test
    fun `Om tiltak ikke inneholder ANNET er ikke tiltaksbeskrivelse påkrevd`() {
        TestData.fullValidRequest.copy(
            tiltak = listOf(Tiltak.TILPASSET_ARBEIDSTID),
            tiltakBeskrivelse = null
        )
    }

    @Test
    internal fun `Bekreftelse av egenerklæring er påkrevd`() {
        validationShouldFailFor(GravidSoknadRequest::bekreftet) {
            TestData.fullValidRequest.copy(bekreftet = false)
        }
    }

    @Test
    fun `Dersom omplassering ikke er mulig må det finnes en årsak`() {
        validationShouldFailFor(GravidSoknadRequest::omplasseringAarsak) {
            TestData.fullValidRequest.copy(
                omplassering = Omplassering.IKKE_MULIG,
                omplasseringAarsak = null
            )
        }

        TestData.fullValidRequest.copy(
            omplassering = Omplassering.IKKE_MULIG,
            omplasseringAarsak = OmplasseringAarsak.FAAR_IKKE_KONTAKT
        )
    }
}