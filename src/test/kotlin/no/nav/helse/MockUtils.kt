package no.nav.helse

import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.domain.GravidSoeknad
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import no.nav.helse.fritakagp.domain.Omplassering
import no.nav.helse.fritakagp.domain.OmplasseringAarsak
import no.nav.helse.fritakagp.domain.Tiltak
import java.time.LocalDate

fun mockGravidSoeknad(): GravidSoeknad =
    GravidSoeknad(
        virksomhetsnummer = GravidTestData.validOrgNr,
        identitetsnummer = GravidTestData.validIdentitetsnummer,
        tilrettelegge = true,
        tiltak = listOf(Tiltak.HJEMMEKONTOR, Tiltak.ANNET),
        tiltakBeskrivelse = GravidTestData.tiltakBeskrivelse,
        omplassering = Omplassering.IKKE_MULIG,
        omplasseringAarsak = OmplasseringAarsak.HELSETILSTANDEN,
        sendtAv = GravidTestData.validIdentitetsnummer,
        termindato = LocalDate.now().plusDays(25),
        sendtAvNavn = GravidTestData.validSendtAvNavn,
        navn = GravidTestData.validNavn
    )

fun mockGravidKrav(): GravidKrav =
    GravidKrav(
        sendtAv = GravidTestData.validIdentitetsnummer,
        virksomhetsnummer = GravidTestData.validOrgNr,
        identitetsnummer = GravidTestData.validIdentitetsnummer,
        perioder = listOf(
            Arbeidsgiverperiode(
                LocalDate.of(2020, 1, 5),
                LocalDate.of(2020, 1, 10),
                5,
                månedsinntekt = 2590.8
            )
        ),
        kontrollDager = null,
        antallDager = 4,
    )

fun mockKroniskSoeknad(): KroniskSoeknad =
    KroniskSoeknad(
        virksomhetsnummer = KroniskTestData.validOrgNr,
        identitetsnummer = KroniskTestData.validIdentitetsnummer,
        fravaer = KroniskTestData.generateFravaersdata(),
        antallPerioder = KroniskTestData.antallPerioder,
        bekreftet = true,
        sendtAv = KroniskTestData.validIdentitetsnummer,
        sendtAvNavn = KroniskTestData.validSendtAvNavn,
        navn = KroniskTestData.validSendtAvNavn
    )

fun mockKroniskKrav(): KroniskKrav =
    KroniskKrav(
        sendtAv = KroniskTestData.validIdentitetsnummer,
        virksomhetsnummer = KroniskTestData.validOrgNr,
        identitetsnummer = KroniskTestData.validIdentitetsnummer,
        perioder = listOf(
            Arbeidsgiverperiode(
                LocalDate.of(2020, 1, 5),
                LocalDate.of(2020, 1, 10),
                5,
                månedsinntekt = 2590.8
            )
        ),
        kontrollDager = null,
        antallDager = 4,
        sendtAvNavn = KroniskTestData.validSendtAvNavn,
        navn = KroniskTestData.validSendtAvNavn
    )
