package no.nav.helse

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.*
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.Prioritet
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.Status
import no.nav.helse.fritakagp.domain.*
import no.nav.helse.fritakagp.web.api.resreq.GravidKravRequest
import no.nav.helse.fritakagp.web.api.resreq.KroniskKravRequest
import no.nav.helse.fritakagp.web.api.resreq.KroniskSoknadRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object AaregTestData {
    val arbeidsgiver = Arbeidsgiver("AS", "1232242423")
    val opplysningspliktig = Opplysningspliktig("AS", "1212121212")

    val evigArbeidsForholdListe = listOf(
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.MIN,
                    LocalDate.MAX
                )
            ),
            LocalDateTime.now()
        )
    )
    val avsluttetArbeidsforholdListe = listOf(
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.MIN,
                    LocalDate.of(2021, 2, 5)
                )
            ),
            LocalDateTime.now()
        )
    )

    val pågåendeArbeidsforholdListe = listOf(
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2021, 2, 5),
                    null
                )
            ),
            LocalDateTime.now()
        )
    )

    val listeMedEttArbeidsforhold = listOf(
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2019, 1, 1),
                    LocalDate.of(2021, 2, 28)
                )
            ),
            LocalDateTime.now()
        ),

        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2021, 3, 1),
                    null
                )
            ),
            LocalDateTime.now()
        )
    )





}
