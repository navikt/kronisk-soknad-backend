package no.nav.helse.fritakagp

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.features.ClientRequestException
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostRequest
import org.slf4j.Logger

fun DokarkivKlient.journalførDokumentNy(
    journalpost: JournalpostRequest,
    callId: String,
    om: ObjectMapper,
    logger: Logger
): String {
    try {
        return this.journalførDokument(journalpost, true, callId).journalpostId
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.Conflict) {
            val journalpostId = runBlocking { om.readTree(e.response.readText()).get("journalpostId").asText() }
            if (!journalpostId.isNullOrEmpty()) {
                logger.info("Fikk 409 konflikt ved journalføring med referanse(id=${journalpost.eksternReferanseId}) og tittel(${journalpost.tittel}, Returnerer journalpostId($journalpostId) likevel.")
                return journalpostId
            }
        }
        throw e
    }
}
