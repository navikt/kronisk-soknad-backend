package no.nav.helse.fritakagp.processing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.db.GravidSoeknadRepository
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.db.KroniskSoeknadRepository
import no.nav.helsearbeidsgiver.utils.logger
import java.time.Duration
import java.time.LocalDateTime

class SlettUtdaterteJob(
    private val gravidSoeknadRepo: GravidSoeknadRepository,
    private val gravidKravRepo: GravidKravRepository,
    private val kroniskSoeknadRepo: KroniskSoeknadRepository,
    private val kroniskKravRepo: KroniskKravRepository,
) : RecurringJob(
    CoroutineScope(Dispatchers.IO),
    Duration.ofDays(1).toMillis(),
) {
    private val logger = this.logger()

    override fun doJob() {
        val oneYearInPast = LocalDateTime.now().minusYears(1)
        val threeYearsInPast = LocalDateTime.now().minusYears(3)

        listOf(
            gravidSoeknadRepo to oneYearInPast,
            gravidKravRepo to oneYearInPast,
            kroniskSoeknadRepo to threeYearsInPast,
            kroniskKravRepo to threeYearsInPast,
        )
            .forEach { (repo, slettOpprettetFoer) ->
                logger.info("Sletter utdaterte rader for tabell '${repo.tableName}'.")
                val amountDeleted = repo.deleteAllOpprettetFoer(slettOpprettetFoer)
                logger.info("Slettet $amountDeleted utdaterte rader for tabell '${repo.tableName}'.")
            }
    }
}
