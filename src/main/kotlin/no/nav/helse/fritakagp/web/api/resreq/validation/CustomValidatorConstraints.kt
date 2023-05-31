package no.nav.helse.fritakagp.web.api.resreq.validation

import no.nav.helse.fritakagp.domain.AgpFelter
import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import no.nav.helse.fritakagp.domain.ArbeidsgiverperiodeNy
import no.nav.helse.fritakagp.domain.FravaerData
import no.nav.helse.fritakagp.domain.GodkjenteFiletyper
import no.nav.helse.fritakagp.domain.KroniskSoeknad
import no.nav.helse.fritakagp.domain.Periode
import org.valiktor.Constraint
import org.valiktor.Validator
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

interface CustomConstraint : Constraint {
    override val messageBundle: String
        get() = "validation/validation-messages"
}

class RefusjonsdagerKanIkkeOverstigePeriodelengdenConstraint : CustomConstraint

fun <E> Validator<E>.Property<Iterable<Periode>?>.refusjonsDagerIkkeOverstigerPeriodelengde(antallDagerMedRefusjon: Int) =
    this.validate(RefusjonsdagerKanIkkeOverstigePeriodelengdenConstraint()) {
        val antallDager = it?.map { ChronoUnit.DAYS.between(it.fom, it.tom.plusDays(1)) }?.sum() ?: 0
        return@validate it != null && antallDager >= antallDagerMedRefusjon
    }

class MåVæreVirksomhetContraint : CustomConstraint

fun <E> Validator<E>.Property<String?>.isVirksomhet(erVirksomhet: Boolean) =
    this.validate(MåVæreVirksomhetContraint()) { erVirksomhet }


class OppholdOverstiger16DagerConstraint : CustomConstraint
fun <E> Validator<E>.Property<Iterable<ArbeidsgiverperiodeNy>?>.oppholdOverstiger16dager() =
    this.validate(OppholdOverstiger16DagerConstraint()) {
        val sortedArbeidsgiverperiodeNy: List<ArbeidsgiverperiodeNy> = it!!.sortedBy {
            it.fraOgMed()
        }.toList()
        val validationResult =
            sortedArbeidsgiverperiodeNy.zipWithNext { a, b ->
                Pair(a.tom, b.fom)
            }.map {
                abs(ChronoUnit.DAYS.between(it.first, it.second))
            }.all {
                it > 16
            }
        return@validate validationResult
    }

class FraDatoKanIkkeKommeEtterTomDato : CustomConstraint

fun <E> Validator<E>.Property<LocalDate?>.datoerHarRiktigRekkefolge(tom: LocalDate) =
    this.validate(FraDatoKanIkkeKommeEtterTomDato()) { fom -> fom!!.isEqual(tom) || fom!!.isBefore(tom) }

class MaanedsInntektErStorreEnTiMil : CustomConstraint

fun <E> Validator<E>.Property<Double?>.maanedsInntektErMellomNullOgTiMil() =
    this.validate(MaanedsInntektErStorreEnTiMil()) {
        it!! > 0.0 && it!! <= TiMil
    }

class AntallPerioderErMellomNullOgTreHundre() : CustomConstraint

fun <E> Validator<E>.Property<Iterable<KroniskSoeknad>?>.antallPerioderErMellomNullOgTreHundre() =
    this.validate(AntallPerioderErMellomNullOgTreHundre()) { ps ->
        ps!!.any { p -> p.antallPerioder >= 1 && p.antallPerioder < 300 }
    }

class DataUrlExtensionConstraints : CustomConstraint

fun <E> Validator<E>.Property<String?>.isGodskjentFiletyper() =
    this.validate(DataUrlExtensionConstraints()) {
        return@validate enumContains<GodkjenteFiletyper>(extractFilExtDel(it!!.uppercase()))
    }

class DataUrlBase64Constraints : CustomConstraint

fun <E> Validator<E>.Property<String?>.isAvStorrelse(minSize: Long, maxSize: Long) =
    this.validate(DataUrlBase64Constraints()) {
        return@validate extractBase64Del(it!!).toByteArray().size <= maxSize && extractBase64Del(it!!).toByteArray().size > minSize
    }

class MaxAgeFravaersDataConstraint : CustomConstraint

fun <E> Validator<E>.Property<Iterable<FravaerData>?>.ingenDataEldreEnn(aar: Long) =
    this.validate(MaxAgeFravaersDataConstraint()) { ps ->
        val minDate = LocalDate.now().minusYears(aar).withDayOfMonth(1)
        return@validate !ps!!.any {
            LocalDate.parse("${it.yearMonth}-01").isBefore(minDate)
        }
    }

class NoFutureFravaersDataConstraint : CustomConstraint

fun <E> Validator<E>.Property<Iterable<FravaerData>?>.ingenDataFraFremtiden() =
    this.validate(NoFutureFravaersDataConstraint()) { ps ->
        val maxDate = LocalDate.now().withDayOfMonth(1)
        return@validate !ps!!.any {
            LocalDate.parse("${it.yearMonth}-01").isAfter(maxDate)
        }
    }

class MaxNumFravaersdagFravaersDataConstraint : CustomConstraint

fun <E> Validator<E>.Property<Iterable<FravaerData>?>.ikkeFlereFravaersdagerEnnDagerIMaanden() =
    this.validate(MaxNumFravaersdagFravaersDataConstraint()) { ps ->
        return@validate !ps!!.any {
            LocalDate.parse("${it.yearMonth}-01").lengthOfMonth() < it.antallDagerMedFravaer
        }
    }

inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
    return enumValues<T>().any { it.name == name }
}

fun extractBase64Del(dataUrl: String): String = dataUrl.substringAfter("base64,")

fun extractFilExtDel(dataUrl: String): String {
    if (!dataUrl.contains(';'))
        return ""
    else
        return dataUrl.substring(0, dataUrl.indexOf(';')).substringAfter('/')
}

class VirusCheckConstraint : CustomConstraint

const val TiMil = 10000000.0
