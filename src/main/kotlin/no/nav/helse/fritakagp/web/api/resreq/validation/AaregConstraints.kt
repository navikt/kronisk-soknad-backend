package no.nav.helse.fritakagp.web.api.resreq.validation

import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.aareg.Arbeidsforhold
import org.valiktor.Validator
import java.time.LocalDate
import no.nav.helsearbeidsgiver.aareg.Periode as AaregPeriode

class ArbeidsforholdConstraint : CustomConstraint
val MAKS_DAGER_OPPHOLD = 3L

fun <E> Validator<E>.Property<LocalDate?>.måHaAktivtArbeidsforhold(agp: Arbeidsgiverperiode, aaregData: List<Arbeidsforhold>) =
    this.validate(ArbeidsforholdConstraint()) {
        val ansattPerioder = slåSammenPerioder(aaregData.map { it.ansettelsesperiode.periode })
        return@validate agp.innenforArbeidsforhold(ansattPerioder) ||
            agp.innenforArbeidsforhold(aaregData.map { it.ansettelsesperiode.periode })
    }

fun Arbeidsgiverperiode.innenforArbeidsforhold(ansattPerioder: List<AaregPeriode>): Boolean {
    return ansattPerioder.any { ansPeriode ->
        (ansPeriode.tom == null || this.tom.isBefore(ansPeriode.tom) || this.tom == ansPeriode.tom) &&
            (ansPeriode.fom!!.isBefore(this.fom) || ansPeriode.fom!!.isEqual(this.fom))
    }
}

fun slåSammenPerioder(list: List<AaregPeriode>): List<AaregPeriode> {
    if (list.size < 2) return list

    val remainingPeriods = list
        .sortedBy { it.fom }
        .toMutableList()

    val merged = ArrayList<AaregPeriode>()

    do {
        var currentPeriod = remainingPeriods[0]
        remainingPeriods.removeAt(0)

        do {
            val connectedPeriod = remainingPeriods
                .find { !oppholdMellomPerioderOverstigerDager(currentPeriod, it, MAKS_DAGER_OPPHOLD) }
            if (connectedPeriod != null) {
                currentPeriod = AaregPeriode(currentPeriod.fom, connectedPeriod.tom)
                remainingPeriods.remove(connectedPeriod)
            }
        } while (connectedPeriod != null)

        merged.add(currentPeriod)
    } while (remainingPeriods.isNotEmpty())

    return merged
}

fun oppholdMellomPerioderOverstigerDager(
    a1: AaregPeriode,
    a2: AaregPeriode,
    dager: Long
): Boolean {
    return a1.tom?.plusDays(dager)?.isBefore(a2.fom) ?: true
}
