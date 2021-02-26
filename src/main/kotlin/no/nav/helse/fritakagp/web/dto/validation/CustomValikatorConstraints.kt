package no.nav.helse.fritakagp.web.dto.validation

import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import no.nav.helse.fritakagp.domain.FravaerData
import no.nav.helse.fritakagp.domain.GodkjenteFiletyper
import org.valiktor.Constraint
import org.valiktor.Validator
import java.time.LocalDate
import java.time.temporal.ChronoUnit

interface CustomConstraint : Constraint {
    override val messageBundle: String
        get() = "validation/validation-messages"
}


class RefusjonsdagerKanIkkeOverstigePeriodelengdenConstraint : CustomConstraint
fun <E> Validator<E>.Property<Arbeidsgiverperiode?>.refusjonsDagerIkkeOverstigerPeriodelengde() =
    this.validate(RefusjonsdagerKanIkkeOverstigePeriodelengdenConstraint()) { ps ->
        return@validate ChronoUnit.DAYS.between(ps?.fom, ps?.tom?.plusDays(1)) >= ps?.antallDagerMedRefusjon!!
    }

fun <E> Validator<E>.Property<Iterable<Arbeidsgiverperiode>?>.refujonsDagerIkkeOverstigerPeriodelengder() =
    this.validate(RefusjonsdagerKanIkkeOverstigePeriodelengdenConstraint()) { ps ->
        !ps!!.any { p ->
            ChronoUnit.DAYS.between(p.fom, p.tom.plusDays(1)) < p.antallDagerMedRefusjon
        }
    }


class DataUrlExtensionConstraints: CustomConstraint
fun <E> Validator<E>.Property<String?>.isGodskjentFiletyper() =
    this.validate(DataUrlExtensionConstraints()){
        return@validate enumContains<GodkjenteFiletyper>(extractFilExtDel(it!!.toUpperCase()))
    }


class DataUrlBase64Constraints : CustomConstraint
fun <E> Validator<E>.Property<String?>.isNotStorreEnn(maxSize: Long) =
    this.validate(DataUrlBase64Constraints()){
        return@validate  extractBase64Del(it!!).toByteArray().size <= maxSize
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
    return enumValues<T>().any { it.name == name}
}

fun extractBase64Del(dataUrl : String) : String = dataUrl.substringAfter("base64,")
fun extractFilExtDel(dataUrl : String) : String = dataUrl.substring(0,dataUrl.indexOf(';')).substringAfter('/')

class VirusCheckConstraint : CustomConstraint
