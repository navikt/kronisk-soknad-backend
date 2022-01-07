package no.nav.helse

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.*
import java.time.LocalDate
import java.time.LocalDateTime

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
    val arbeidsforholdMedSluttDato = listOf(
        Arbeidsforhold(
            arbeidsgiver,
            opplysningspliktig,
            emptyList(),
            Ansettelsesperiode(
                Periode(
                    LocalDate.of(2004, 6, 1),
                    LocalDate.of(2004, 6, 30),
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
                    LocalDate.of(2004, 9, 1),
                    LocalDate.of(2004, 9, 30)
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
                    LocalDate.of(2005, 1, 1),
                    LocalDate.of(2005, 2, 28)
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
                    LocalDate.of(2005, 9, 6),
                    LocalDate.of(2007, 12, 31)
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
                    LocalDate.of(2008, 6, 16),
                    LocalDate.of(2008, 8, 3)
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
                    LocalDate.of(2009, 3, 5),
                    LocalDate.of(2010, 8, 30 )
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
                    LocalDate.of(2010, 11, 26),
                    LocalDate.of(2011, 9, 4 )
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
                    LocalDate.of(2011, 9, 5),
                    LocalDate.of(2013, 3, 30 )
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
                    LocalDate.of(2013, 3, 31),
                    LocalDate.of(2014, 1, 1 )
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
                    LocalDate.of(2013, 3, 31),
                    LocalDate.of(2013, 3, 31 )
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
                    LocalDate.of(2014, 2, 24),
                    LocalDate.of(2014, 2, 24 )
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
                    LocalDate.of(2014, 3, 28),
                    LocalDate.of(2014, 5, 31 )
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
                    LocalDate.of(2014, 6, 1),
                    LocalDate.of(2022, 4, 30 )
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
                    LocalDate.of(2014, 6, 1),
                    LocalDate.of(2014, 12, 31 )
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
                    LocalDate.of(2022, 5, 1),
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
