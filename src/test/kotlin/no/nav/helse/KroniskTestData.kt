package no.nav.helse

import no.nav.helse.fritakagp.domain.*
import no.nav.helse.fritakagp.web.api.resreq.KroniskSoknadRequest

object KroniskTestData {
    val validIdentitetsnummer = "20015001543"
    val validOrgNr = "123456785"

    val soeknadKronisk = SoeknadKronisk(
        orgnr = validOrgNr,
        fnr = validIdentitetsnummer,
        arbeid = listOf(ArbeidsType.KREVENDE, ArbeidsType.MODERAT),
        bekreftet = true,
        fravaer = listOf(FravaerData("2020-4", "3"), FravaerData("2019-6", "7")),
        paakjenninger = listOf(PaakjenningsType.ALLERGENER, PaakjenningsType.TUNGE),
        sendtAv = "09876543210"
    )

    val fullValidRequest = KroniskSoknadRequest(
        orgnr = validOrgNr,
        fnr = validIdentitetsnummer,
        arbeidstyper = listOf(ArbeidsType.KREVENDE, ArbeidsType.MODERAT, ArbeidsType.STILLESITTENDE),
        fravaer = listOf(FravaerData("2020-4", "3"), FravaerData("2019-6", "7")),
        paakjenningstyper = listOf(PaakjenningsType.ALLERGENER, PaakjenningsType.TUNGE, PaakjenningsType.ANNET, PaakjenningsType.GAAING, PaakjenningsType.HARDE, PaakjenningsType.REGELMESSIG, PaakjenningsType.STRESSENDE, PaakjenningsType.UKOMFORTABEL),
        paakjenningBeskrivelse = "Lorem Ipsum",
        bekreftet = true,
        dokumentasjon = null
    )

    val kroniskSoknadMedFil = KroniskSoknadRequest(
        orgnr = KroniskTestData.validOrgNr,
        fnr = KroniskTestData.validIdentitetsnummer,
        arbeidstyper = listOf(ArbeidsType.MODERAT),
        fravaer = listOf(FravaerData("2020-4", "3"), FravaerData("2019-6", "7")),
        paakjenningstyper = listOf(PaakjenningsType.ALLERGENER, PaakjenningsType.TUNGE),
        bekreftet = true,
        dokumentasjon = """
                data:image/jpeg;base64,TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVyIGFkaXBpc2NpbmcgZWxpdC4gQWxpcXVhbSB2aXRhZSBlcm9zIGEgZmVsaXMgbGFjaW5pYSBzb2xsaWNpdHVkaW4gdXQgZWdldCB0b3J0b3IuIFBoYXNlbGx1cyB2ZWhpY3VsYSBlZ2VzdGFzIG1hdHRpcy4gTnVuYyBldSBsaWJlcm8gdWxsYW1jb3JwZXIsIHBsYWNlcmF0IHNhcGllbiBlZ2V0LCBhY2N1bXNhbiBwdXJ1cy4gTWFlY2VuYXMgbWF4aW11cywgcHVydXMgbmVjIGxhY2luaWEgcHVsdmluYXIsIGR1aSBlbmltIGlhY3VsaXMgZGlhbSwgcXVpcyB2aXZlcnJhIG1hc3NhIGxpZ3VsYSBzaXQgYW1ldCBudWxsYS4gU2VkIG1heGltdXMgZXVpc21vZCBhbnRlIGluIHBvc3VlcmUuIFN1c3BlbmRpc3NlIGxpZ3VsYSB0ZWxsdXMsIGZpbmlidXMgdmVsIHBsYWNlcmF0IGlkLCBtYXhpbXVzIHNlZCBhbnRlLiBGdXNjZSBzaXQgYW1ldCBmZXJtZW50dW0gbWFnbmEuCgpDbGFzcyBhcHRlbnQgdGFjaXRpIHNvY2lvc3F1IGFkIGxpdG9yYSB0b3JxdWVudCBwZXIgY29udWJpYSBub3N0cmEsIHBlciBpbmNlcHRvcyBoaW1lbmFlb3MuIERvbmVjIGV1IHRvcnRvciBtYWxlc3VhZGEsIHVsbGFtY29ycGVyIG5pc2wgYXQsIHZ1bHB1dGF0ZSBlc3QuIFZpdmFtdXMgaWQgbG9yZW0gZWdlc3RhcyBhcmN1IHNvZGFsZXMgc2VtcGVyIHZpdGFlIHZlc3RpYnVsdW0gZG9sb3IuIENyYXMgZGFwaWJ1cywgZXJhdCBuZWMgZmF1Y2lidXMgZGFwaWJ1cywgZHVpIHZlbGl0IG9ybmFyZSB0ZWxsdXMsIHF1aXMgdWx0cmljaWVzIGxlbyB0ZWxsdXMgdXQgZXJhdC4gTWFlY2VuYXMgcG9ydGEgdGluY2lkdW50IHBsYWNlcmF0LiBDcmFzIGRpZ25pc3NpbSBsZWN0dXMgdGVsbHVzLCBldCBpbnRlcmR1bSByaXN1cyBwZWxsZW50ZXNxdWUgYXVjdG9yLiBJbiBtYXhpbXVzIGxhY2luaWEgbGVjdHVzLCBhIHNvZGFsZXMgbnVsbGEgdmFyaXVzIGdyYXZpZGEuIEV0aWFtIGhlbmRyZXJpdCBhdWd1ZSBvZGlvLCB2ZWwgcGhhcmV0cmEgb3JjaSBtYWxlc3VhZGEgbmVjLiBQZWxsZW50ZXNxdWUgaGFiaXRhbnQgbW9yYmkgdHJpc3RpcXVlIHNlbmVjdHVzIGV0IG5ldHVzIGV0IG1hbGVzdWFkYSBmYW1lcyBhYyB0dXJwaXMgZWdlc3Rhcy4gU2VkIGV0IGNvbmRpbWVudHVtIG9yY2ksIHZlbCBtYWxlc3VhZGEgbmVxdWUu
            """.trimIndent()
    )
}

