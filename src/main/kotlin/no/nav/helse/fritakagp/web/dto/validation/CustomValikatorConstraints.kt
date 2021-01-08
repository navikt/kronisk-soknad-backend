package no.nav.helse.fritakagp.web.dto.validation

import no.nav.helse.fritakagp.domain.FravaerData
import no.nav.helse.fritakagp.domain.GodskjentFiletyper
import org.valiktor.Constraint
import org.valiktor.Validator
import java.time.LocalDate

interface CustomConstraint : Constraint {
    override val messageBundle: String
        get() = "validation/validation-messages"
}

class DataUrlExtensionConstraints: CustomConstraint
fun <E> Validator<E>.Property<String?>.isGodskjentFiletyper() =
    this.validate(DataUrlExtensionConstraints()){
        return@validate enumContains<GodskjentFiletyper>(extractFilExtDel(it!!.toUpperCase()))
    }


class DataUrlBase64Constraints : CustomConstraint
fun <E> Validator<E>.Property<String?>.isNotStorreEnn(maxSize: Long) =
    this.validate(DataUrlBase64Constraints()){
        return@validate  extractBase64Del(it!!).toByteArray().size <= maxSize
    }


class MaxAgeFravaersDataConstraint : CustomConstraint
fun <E> Validator<E>.Property<Iterable<FravaerData>?>.ingenDataEldreEnn(aar: Long) =
    this.validate(MaxAgeFravaersDataConstraint()) { ps ->
        val minDate = LocalDate.now().minusYears(aar)
        return@validate !ps!!.any {
            LocalDate.parse("${it.yearMonth}-01").isBefore(minDate)
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

