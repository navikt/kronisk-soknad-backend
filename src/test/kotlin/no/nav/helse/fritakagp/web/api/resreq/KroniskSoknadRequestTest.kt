package no.nav.helse.fritakagp.web.api.resreq

import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.domain.OmplasseringAarsak
import no.nav.helse.fritakagp.domain.Tiltak
import org.junit.jupiter.api.Test

internal class KroniskSoknadRequestTest {

    @Test
    internal fun `Gyldig FNR er påkrevd`() {
        validationShouldFailFor(KroniskSoknadRequest::fnr) {
            GravidTestData.fullValidRequest.copy(fnr = "01020312345")
        }
    }

    @Test
    internal fun `Gyldig OrgNr er påkrevd dersom det er oppgitt`() {
        validationShouldFailFor(KroniskSoknadRequest::orgnr) {
            GravidTestData.fullValidRequest.copy(orgnr = "098765432")
        }
    }

    @Test
    internal fun `Bekreftelse av egenerklæring er påkrevd`() {
        validationShouldFailFor(KroniskSoknadRequest::bekreftet) {
            GravidTestData.fullValidRequest.copy(bekreftet = false)
        }
    }

    @Test
    fun `Dersom omplassering ikke er mulig må det finnes en årsak`() {

    }
}