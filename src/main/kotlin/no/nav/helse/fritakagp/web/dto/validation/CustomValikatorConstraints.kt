package no.nav.helse.fritakagp.web.dto.validation

import no.nav.helse.fritakagp.domain.OmplasseringAarsak
import no.nav.helse.fritakagp.domain.Tiltak
import org.valiktor.Constraint
import org.valiktor.Validator

interface CustomConstraint : Constraint {
    override val messageBundle: String
        get() = "validation/validation-messages"
}

class TiltakBeskrivelseConstraint : CustomConstraint
fun <E> Validator<E>.Property<Iterable<String>?>.isTiltakValid(beskrivelse : String?) =
        this.validate(TiltakBeskrivelseConstraint()) { ps ->
           if(ps!!.contains(Tiltak.ANNET.name))
               return@validate !beskrivelse.isNullOrEmpty()
            else
               return@validate true
        }

class OmplasseringConstraints : CustomConstraint
fun <E> Validator<E>.Property<String?>.isOmplasseringValgRiktig(omplassering : String) =
        this.validate(OmplasseringConstraints()) {
            if (omplassering.toUpperCase() == "IKKE_MULIG")
                return@validate enumContains<OmplasseringAarsak>(it!!)
            else
                return@validate true
        }

class FileSizeConstraints : CustomConstraint
fun <E> Validator<E>.Property<String?>.isNotStorreEnn(maxSize : Long) =
    this.validate(FileSizeConstraints()){
        return@validate  it!!.toByteArray().size <= maxSize
    }



inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
    return enumValues<T>().any { it.name == name}
}