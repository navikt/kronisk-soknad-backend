package no.nav.helse.fritakagp

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostRequest
import org.slf4j.Logger

fun DokarkivKlient.journalførOgFerdigstillDokument(
    journalpost: JournalpostRequest,
    callId: String,
    om: ObjectMapper,
    logger: Logger
): String {
    return try {
        this.journalførDokument(journalpost, true, callId).journalpostId
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.Conflict) {
            val journalpostId = runBlocking { e.response.bodyAsText().let(om::readTree).get("journalpostId").asText() }
            if (!journalpostId.isNullOrEmpty()) {
                logger.info("Fikk 409 Conflict ved journalføring med referanse(id=${journalpost.eksternReferanseId}) og tittel (${journalpost.tittel}. Bruker eksisterende journalpostId ($journalpostId) fra respons.")
                return journalpostId
            }
        }
        throw e
    }
}
