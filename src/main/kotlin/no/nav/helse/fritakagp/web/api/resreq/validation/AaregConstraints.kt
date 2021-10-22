package no.nav.helse.fritakagp.web.api.resreq.validation

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import org.valiktor.Validator
import java.time.LocalDate
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Periode as AaregPeriode

class ArbeidsforholdConstraint : CustomConstraint

fun <E> Validator<E>.Property<LocalDate?>.måHaAktivtArbeidsforhold(agp: Arbeidsgiverperiode, aaregData: List<Arbeidsforhold>) =
    this.validate(ArbeidsforholdConstraint()) {
        val ansattPerioder = slåSammenPerioder(aaregData.map { it.ansettelsesperiode.periode })

        return@validate ansattPerioder.any { ansPeriode ->
            (ansPeriode.tom == null || agp.tom.isBefore(ansPeriode.tom) || agp.tom == ansPeriode.tom)
                && ansPeriode.fom!!.isBefore(agp.fom)
        }
    }

fun slåSammenPerioder(list: List<AaregPeriode>): List<AaregPeriode> {
    if (list.size < 2) return list

    val periods = list
        .sortedWith(compareBy(AaregPeriode::fom, AaregPeriode::tom))

    val merged = mutableListOf<AaregPeriode>()

    periods.forEach { gjeldendePeriode ->
        // Legg til første periode
        if (merged.size == 0) {
            merged.add(gjeldendePeriode)
            return@forEach
        }

        val forrigePeriode = merged.last()
        // Hvis periode overlapper, oppdater tom
        if (overlapperPeriode(gjeldendePeriode, forrigePeriode)) {
            merged[merged.lastIndex] = AaregPeriode(forrigePeriode.fom, gjeldendePeriode.tom)
            return@forEach
        }

        merged.add(gjeldendePeriode)
    }

    return merged
}

fun overlapperPeriode(
    gjeldendePeriode: AaregPeriode,
    forrigePeriode: AaregPeriode,
    dager: Long = 3L // MAKS_DAGER_OPPHOLD
): Boolean {
    return (
        gjeldendePeriode.fom!!.isBefore(forrigePeriode.tom?.plusDays(dager))
            || gjeldendePeriode.fom!!.isEqual(forrigePeriode.tom?.plusDays(dager))
        )
}
