package no.nav.helse.fritakagp.web.dto.validation

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import org.valiktor.Validator
import java.time.LocalDate
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Periode as AaregPeriode

class ArbeidsforholdConstraint : CustomConstraint
val MAKS_DAGER_OPPHOLD = 3L

fun <E> Validator<E>.Property<LocalDate?>.måHaAktivtArbeidsforhold(agp: Arbeidsgiverperiode, aaregData: List<Arbeidsforhold>) =
    this.validate(ArbeidsforholdConstraint()) {
        val sisteArbeidsforhold = aaregData
            .sortedBy { it.ansettelsesperiode.periode.tom ?: LocalDate.MAX }
            .takeLast(2)

        val ansPeriode = if (sisteArbeidsforhold.size <= 1 || oppholdMellomPerioderOverstigerDager(sisteArbeidsforhold, MAKS_DAGER_OPPHOLD))
            sisteArbeidsforhold.lastOrNull()?.ansettelsesperiode?.periode ?: AaregPeriode(LocalDate.MAX, LocalDate.MAX)
        else
            AaregPeriode(
                sisteArbeidsforhold.first().ansettelsesperiode.periode.fom,
                sisteArbeidsforhold.last().ansettelsesperiode.periode.tom
            )

        val kravPeriodeSubsettAvAnsPeriode = ansPeriode.tom == null ||
                agp.tom.isBefore(ansPeriode.tom) ||
                agp.tom == ansPeriode.tom

        return@validate kravPeriodeSubsettAvAnsPeriode
    }

fun oppholdMellomPerioderOverstigerDager(sisteArbeidsforhold: List<Arbeidsforhold>, dager: Long): Boolean {
    return sisteArbeidsforhold.first().ansettelsesperiode.periode.tom?.plusDays(dager)
        ?.isBefore(sisteArbeidsforhold.last().ansettelsesperiode.periode.fom) ?: true
}
