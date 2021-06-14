package no.nav.helse

import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.Prioritet
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.Status
import no.nav.helse.fritakagp.domain.*
import no.nav.helse.fritakagp.web.api.resreq.GravidKravRequest
import no.nav.helse.fritakagp.web.api.resreq.GravidSoknadRequest
import java.time.LocalDate

object GravidTestData {
    val validIdentitetsnummer = "20015001543"
    val validOrgNr = "917404437"

    val soeknadGravid = GravidSoeknad(
        virksomhetsnummer = validOrgNr,
        identitetsnummer = validIdentitetsnummer,
        termindato = LocalDate.now().plusDays(25),
        tilrettelegge = true,
        tiltak = listOf(Tiltak.HJEMMEKONTOR, Tiltak.ANNET),
        tiltakBeskrivelse = "Vi prøvde både det ene og det andre og det første kanskje virka litt men muligens and the andre ikke var så på stell men akk ja sånn lorem",
        omplassering = Omplassering.IKKE_MULIG,
        omplasseringAarsak = OmplasseringAarsak.HELSETILSTANDEN,
        sendtAv = "09876543210"
    )

    val fullValidSoeknadRequest = GravidSoknadRequest(
        virksomhetsnummer = validOrgNr,
        identitetsnummer = validIdentitetsnummer,
        termindato = LocalDate.now().plusDays(25),
        tilrettelegge = true,
        tiltak = listOf(
            Tiltak.ANNET,
            Tiltak.HJEMMEKONTOR,
            Tiltak.TILPASSEDE_ARBEIDSOPPGAVER,
            Tiltak.TILPASSET_ARBEIDSTID
        ),
        tiltakBeskrivelse = "beskrivelse",
        omplassering = Omplassering.NEI,
        omplasseringAarsak = OmplasseringAarsak.HELSETILSTANDEN,
        bekreftet = true,
        dokumentasjon = null
    )

    val gravidSoknadMedFil = GravidSoknadRequest(
        virksomhetsnummer = GravidTestData.validOrgNr,
        identitetsnummer = GravidTestData.validIdentitetsnummer,
        termindato = LocalDate.now().plusDays(25),
        tilrettelegge = true,
        tiltak = listOf(Tiltak.ANNET),
        tiltakBeskrivelse = "beskrivelse",
        omplassering = Omplassering.JA,
        omplasseringAarsak = null,
        bekreftet = true,
        dokumentasjon = """
                data:image/pdf;base64,TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVyIGFkaXBpc2NpbmcgZWxpdC4gQWxpcXVhbSB2aXRhZSBlcm9zIGEgZmVsaXMgbGFjaW5pYSBzb2xsaWNpdHVkaW4gdXQgZWdldCB0b3J0b3IuIFBoYXNlbGx1cyB2ZWhpY3VsYSBlZ2VzdGFzIG1hdHRpcy4gTnVuYyBldSBsaWJlcm8gdWxsYW1jb3JwZXIsIHBsYWNlcmF0IHNhcGllbiBlZ2V0LCBhY2N1bXNhbiBwdXJ1cy4gTWFlY2VuYXMgbWF4aW11cywgcHVydXMgbmVjIGxhY2luaWEgcHVsdmluYXIsIGR1aSBlbmltIGlhY3VsaXMgZGlhbSwgcXVpcyB2aXZlcnJhIG1hc3NhIGxpZ3VsYSBzaXQgYW1ldCBudWxsYS4gU2VkIG1heGltdXMgZXVpc21vZCBhbnRlIGluIHBvc3VlcmUuIFN1c3BlbmRpc3NlIGxpZ3VsYSB0ZWxsdXMsIGZpbmlidXMgdmVsIHBsYWNlcmF0IGlkLCBtYXhpbXVzIHNlZCBhbnRlLiBGdXNjZSBzaXQgYW1ldCBmZXJtZW50dW0gbWFnbmEuCgpDbGFzcyBhcHRlbnQgdGFjaXRpIHNvY2lvc3F1IGFkIGxpdG9yYSB0b3JxdWVudCBwZXIgY29udWJpYSBub3N0cmEsIHBlciBpbmNlcHRvcyBoaW1lbmFlb3MuIERvbmVjIGV1IHRvcnRvciBtYWxlc3VhZGEsIHVsbGFtY29ycGVyIG5pc2wgYXQsIHZ1bHB1dGF0ZSBlc3QuIFZpdmFtdXMgaWQgbG9yZW0gZWdlc3RhcyBhcmN1IHNvZGFsZXMgc2VtcGVyIHZpdGFlIHZlc3RpYnVsdW0gZG9sb3IuIENyYXMgZGFwaWJ1cywgZXJhdCBuZWMgZmF1Y2lidXMgZGFwaWJ1cywgZHVpIHZlbGl0IG9ybmFyZSB0ZWxsdXMsIHF1aXMgdWx0cmljaWVzIGxlbyB0ZWxsdXMgdXQgZXJhdC4gTWFlY2VuYXMgcG9ydGEgdGluY2lkdW50IHBsYWNlcmF0LiBDcmFzIGRpZ25pc3NpbSBsZWN0dXMgdGVsbHVzLCBldCBpbnRlcmR1bSByaXN1cyBwZWxsZW50ZXNxdWUgYXVjdG9yLiBJbiBtYXhpbXVzIGxhY2luaWEgbGVjdHVzLCBhIHNvZGFsZXMgbnVsbGEgdmFyaXVzIGdyYXZpZGEuIEV0aWFtIGhlbmRyZXJpdCBhdWd1ZSBvZGlvLCB2ZWwgcGhhcmV0cmEgb3JjaSBtYWxlc3VhZGEgbmVjLiBQZWxsZW50ZXNxdWUgaGFiaXRhbnQgbW9yYmkgdHJpc3RpcXVlIHNlbmVjdHVzIGV0IG5ldHVzIGV0IG1hbGVzdWFkYSBmYW1lcyBhYyB0dXJwaXMgZWdlc3Rhcy4gU2VkIGV0IGNvbmRpbWVudHVtIG9yY2ksIHZlbCBtYWxlc3VhZGEgbmVxdWUu
            """.trimIndent()
    )

    val gravidKravRequestValid = GravidKravRequest(
        virksomhetsnummer = validOrgNr,
        identitetsnummer = validIdentitetsnummer,
        perioder = setOf(
            Arbeidsgiverperiode(
                LocalDate.of(2020, 1, 5),
                LocalDate.of(2020, 1, 10),
                2,
                2590.8
            )
        ),
        bekreftet = true,
        dokumentasjon = null,
        kontrollDager = null
    )

    val gravidKravRequestMedFil = gravidKravRequestValid.copy(
        dokumentasjon = """
                data:image/pdf;base64,TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVyIGFkaXBpc2NpbmcgZWxpdC4gQWxpcXVhbSB2aXRhZSBlcm9zIGEgZmVsaXMgbGFjaW5pYSBzb2xsaWNpdHVkaW4gdXQgZWdldCB0b3J0b3IuIFBoYXNlbGx1cyB2ZWhpY3VsYSBlZ2VzdGFzIG1hdHRpcy4gTnVuYyBldSBsaWJlcm8gdWxsYW1jb3JwZXIsIHBsYWNlcmF0IHNhcGllbiBlZ2V0LCBhY2N1bXNhbiBwdXJ1cy4gTWFlY2VuYXMgbWF4aW11cywgcHVydXMgbmVjIGxhY2luaWEgcHVsdmluYXIsIGR1aSBlbmltIGlhY3VsaXMgZGlhbSwgcXVpcyB2aXZlcnJhIG1hc3NhIGxpZ3VsYSBzaXQgYW1ldCBudWxsYS4gU2VkIG1heGltdXMgZXVpc21vZCBhbnRlIGluIHBvc3VlcmUuIFN1c3BlbmRpc3NlIGxpZ3VsYSB0ZWxsdXMsIGZpbmlidXMgdmVsIHBsYWNlcmF0IGlkLCBtYXhpbXVzIHNlZCBhbnRlLiBGdXNjZSBzaXQgYW1ldCBmZXJtZW50dW0gbWFnbmEuCgpDbGFzcyBhcHRlbnQgdGFjaXRpIHNvY2lvc3F1IGFkIGxpdG9yYSB0b3JxdWVudCBwZXIgY29udWJpYSBub3N0cmEsIHBlciBpbmNlcHRvcyBoaW1lbmFlb3MuIERvbmVjIGV1IHRvcnRvciBtYWxlc3VhZGEsIHVsbGFtY29ycGVyIG5pc2wgYXQsIHZ1bHB1dGF0ZSBlc3QuIFZpdmFtdXMgaWQgbG9yZW0gZWdlc3RhcyBhcmN1IHNvZGFsZXMgc2VtcGVyIHZpdGFlIHZlc3RpYnVsdW0gZG9sb3IuIENyYXMgZGFwaWJ1cywgZXJhdCBuZWMgZmF1Y2lidXMgZGFwaWJ1cywgZHVpIHZlbGl0IG9ybmFyZSB0ZWxsdXMsIHF1aXMgdWx0cmljaWVzIGxlbyB0ZWxsdXMgdXQgZXJhdC4gTWFlY2VuYXMgcG9ydGEgdGluY2lkdW50IHBsYWNlcmF0LiBDcmFzIGRpZ25pc3NpbSBsZWN0dXMgdGVsbHVzLCBldCBpbnRlcmR1bSByaXN1cyBwZWxsZW50ZXNxdWUgYXVjdG9yLiBJbiBtYXhpbXVzIGxhY2luaWEgbGVjdHVzLCBhIHNvZGFsZXMgbnVsbGEgdmFyaXVzIGdyYXZpZGEuIEV0aWFtIGhlbmRyZXJpdCBhdWd1ZSBvZGlvLCB2ZWwgcGhhcmV0cmEgb3JjaSBtYWxlc3VhZGEgbmVjLiBQZWxsZW50ZXNxdWUgaGFiaXRhbnQgbW9yYmkgdHJpc3RpcXVlIHNlbmVjdHVzIGV0IG5ldHVzIGV0IG1hbGVzdWFkYSBmYW1lcyBhYyB0dXJwaXMgZWdlc3Rhcy4gU2VkIGV0IGNvbmRpbWVudHVtIG9yY2ksIHZlbCBtYWxlc3VhZGEgbmVxdWUu
            """.trimIndent()
    )

    //Innrapportert feil POH-693 Gravid Krav - Backendvalidereing feiler når man sender inn krav for bare en dag.
    val gravidKravRequestValidPeriode1Dag = GravidKravRequest(
        virksomhetsnummer = validOrgNr,
        identitetsnummer = validIdentitetsnummer,
        perioder = setOf(
            Arbeidsgiverperiode(
                LocalDate.of(2020, 2, 1),
                LocalDate.of(2020, 2, 1),
                1,
                123.8
            )
        ),
        bekreftet = true,
        dokumentasjon = null,
        kontrollDager = null
    )


    val gravidKrav = GravidKrav(
        virksomhetsnummer = validOrgNr,
        identitetsnummer = validIdentitetsnummer,
        perioder = setOf(
            Arbeidsgiverperiode(
                LocalDate.of(2020, 1, 5),
                LocalDate.of(2020, 1, 10),
                5,
                2590.8
            )
        ),
        sendtAv = validIdentitetsnummer,
        kontrollDager = null
    )

    val gravidOpprettOpgaveResponse = OpprettOppgaveResponse(
        id = 1234,
        tildeltEnhetsnr = "0100",
        tema = "KON",
        oppgavetype = "JFR",
        versjon = 1,
        aktivDato = LocalDate.now(),
        Prioritet.NORM,
        Status.UNDER_BEHANDLING
    )
}

