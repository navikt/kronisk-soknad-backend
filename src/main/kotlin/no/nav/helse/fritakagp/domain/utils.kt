package no.nav.helse.fritakagp.domain

import de.m3y.kformat.Table
import de.m3y.kformat.table
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

fun decodeBase64File(datafile: String): ByteArray {
    return Base64.getDecoder().decode(datafile)
}

enum class GodkjenteFiletyper(val beskrivelse: String) {
    PDF("pdf")
}

val SOEKAND_BESKRIVELSE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun sladdFnr(fnr: String): String {
    return fnr.take(6) + "*****"
}

val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
val DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun generereGravidSoeknadBeskrivelse(soeknad: GravidSoeknad, desc: String): String {
    val terminaDatoIkkeOppgitt = "Ikke oppgitt"
    return buildString {
        appendLine(desc)
        appendLine("Mottatt: ${soeknad.opprettet.format(TIMESTAMP_FORMAT)}")
        appendLine("Person (FNR): ${soeknad.identitetsnummer}")
        appendLine("Termindato: ${soeknad.termindato?.format(SOEKAND_BESKRIVELSE_DATE_FORMAT) ?: terminaDatoIkkeOppgitt}")
        appendLine("Arbeidsgiver oppgitt i søknad: ${soeknad.virksomhetsnavn} (${soeknad.virksomhetsnummer}")
        appendLine("Har dere prøvd å tilrettelegge arbeidsdagen slik at den gravide kan jobbe til tross for helseplagene?")
        if (soeknad.tilrettelegge) {
            appendLine("Ja")
            appendLine("Hvilke tiltak har dere forsøkt eller vurdert for at den ansatte kan jobbe?")
            (soeknad.tiltak ?: emptyList()).forEach {
                appendLine(" - ${it.beskrivelse}")
            }
            soeknad.tiltakBeskrivelse?.let { appendLine(it) }
            appendLine("Har dere forsøkt omplassering til en annen jobb?")
            appendLine(soeknad.omplassering?.beskrivelse ?: "")
            soeknad.omplasseringAarsak?.let { appendLine(it.beskrivelse) }
        } else {
            appendLine("Nei")
        }
    }
}

fun generereKroniskSoeknadBeskrivelse(soeknad: KroniskSoeknad, desc: String): String {
    return buildString {
        appendLine(desc)
        appendLine("Mottatt: ${soeknad.opprettet.format(TIMESTAMP_FORMAT)}")
        appendLine("Person (FNR): ${soeknad.identitetsnummer}")
        appendLine("Arbeidsgiver oppgitt i søknad: ${soeknad.virksomhetsnavn} (${soeknad.virksomhetsnummer})")
        if (soeknad.ikkeHistoriskFravaer) {
            appendLine("Det finnes ikke historisk fravær på grunn av nyansettelse, lengre permisjon eller annet.")
        } else {
            val totaltAntallDager = soeknad.fravaer.map { it.antallDagerMedFravaer }.sum()
            appendLine("Totalt antall fraværsdager siste 2 år: $totaltAntallDager")
            appendLine("Antall fraværsperioder siste 2 år: ${soeknad.antallPerioder}")
            appendLine("Fraværsdager per måned siste 2 år:")

            val yearlyFravaer = soeknad.fravaer.sortedByDescending { it.yearMonth }.groupBy { it.yearMonth.substring(0, 4) }
            yearlyFravaer.forEach { yearGroup ->
                appendLine(yearGroup.key)
                yearGroup.value.sortedBy { it.yearMonth }.forEach {
                    appendLine("${it.toLocalDate().month.name}: ${it.antallDagerMedFravaer}   ")
                }
            }
        }
    }
}

fun generereKroniskKravBeskrivelse(krav: KroniskKrav, desc: String): String {
    return buildString {
        appendLine(desc)
        appendLine("Mottatt: ${krav.opprettet.format(TIMESTAMP_FORMAT)}")
        appendLine("Person (FNR): ${krav.identitetsnummer}")
        appendLine("Arbeidsgiver oppgitt i krav: ${krav.virksomhetsnavn} (${krav.virksomhetsnummer})")
        appendLine("Antall lønnsdager: ${krav.antallDager}")
        appendLine("Periode:")
        appendLine(genererePeriodeTable(krav.perioder))
    }
}

fun generereSlettKroniskKravBeskrivelse(krav: KroniskKrav, desc: String): String {
    return buildString {
        appendLine(desc)
        appendLine("Annullering mottatt: ${TIMESTAMP_FORMAT.format(krav.endretDato ?: LocalDateTime.now())}")
        appendLine("Tidligere krav med JournalpostId: ${krav.journalpostId}")
        appendLine("Person (FNR): ${krav.identitetsnummer}")
        appendLine("Arbeidsgiver oppgitt i krav: ${krav.virksomhetsnavn} (${krav.virksomhetsnummer})")
        appendLine("Periode:")
        appendLine(genererePeriodeTable(krav.perioder))
    }
}

fun generereGravidkKravBeskrivelse(krav: GravidKrav, desc: String): String {
    return buildString {
        appendLine(desc)
        appendLine("Mottatt: ${krav.opprettet.format(TIMESTAMP_FORMAT)}")
        appendLine("Person (FNR): ${krav.identitetsnummer}")
        appendLine("Arbeidsgiver oppgitt i krav: ${krav.virksomhetsnavn} (${krav.virksomhetsnummer})")
        appendLine("Antall lønnsdager: ${krav.antallDager}")
        appendLine("Periode:")
        appendLine(genererePeriodeTable(krav.perioder))
    }
}

fun generereSlettGravidKravBeskrivelse(krav: GravidKrav, desc: String): String {
    return buildString {
        appendLine(desc)
        appendLine("Annullering mottatt: ${TIMESTAMP_FORMAT.format(krav.endretDato ?: LocalDateTime.now())}")
        appendLine("Tidligere krav med JournalpostId: ${krav.journalpostId}")
        appendLine("Person (FNR): ${krav.identitetsnummer}")
        appendLine("Arbeidsgiver oppgitt i krav: ${krav.virksomhetsnavn} (${krav.virksomhetsnummer})")
        appendLine("Periode:")
        appendLine(genererePeriodeTable(krav.perioder))
    }
}

fun genererePeriodeTable(perioder: List<Arbeidsgiverperiode>): String {
    return table {
        header("FOM", "TOM", "Sykmeldingsgrad", "kreves refusjon for", "Beregnet månedsinntekt (NOK)", "Dagsats (NOK)", "Beløp (NOK)")
        for (p in perioder) {
            val gradering = (p.gradering * 100).toString() + "%"
            row(
                p.fom.atStartOfDay(),
                p.tom.atStartOfDay(),
                gradering,
                p.antallDagerMedRefusjon,
                p.månedsinntekt.toString(),
                p.dagsats.toString(),
                p.belop.toString()
            )
        }
        hints {
            alignment("FOM", Table.Hints.Alignment.LEFT)
            alignment("TOM", Table.Hints.Alignment.LEFT)
            alignment("Sykmeldingsgrad", Table.Hints.Alignment.LEFT)
            alignment("kreves refusjon for", Table.Hints.Alignment.LEFT)
            alignment("Beregnet månedsinntekt (NOK)", Table.Hints.Alignment.LEFT)
            alignment("Dagsats (NOK)", Table.Hints.Alignment.LEFT)
            alignment("Beløp (NOK)", Table.Hints.Alignment.LEFT)
            borderStyle = Table.BorderStyle.SINGLE_LINE
        }
    }.render(StringBuilder()).toString()
}
