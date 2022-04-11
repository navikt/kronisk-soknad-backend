package no.nav.helse.fritakagp.processing

import no.nav.helse.fritakagp.domain.decodeBase64File
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.integration.virusscan.VirusScanner
import no.nav.helse.fritakagp.web.api.resreq.validation.VirusCheckConstraint
import no.nav.helse.fritakagp.web.api.resreq.validation.extractBase64Del
import no.nav.helse.fritakagp.web.api.resreq.validation.extractFilExtDel
import org.valiktor.ConstraintViolationException
import org.valiktor.DefaultConstraintViolation
import java.util.*

class GcpOpplasting(
    private val virusScanner: VirusScanner,
    private val bucket: BucketStorage
) {

    suspend fun processDocumentForGCPStorage(doc: String?, id: UUID) {

        if (!doc.isNullOrEmpty()) {
            val fileContent = extractBase64Del(doc)
            val fileExt = extractFilExtDel(doc)
            if (!virusScanner.scanDoc(decodeBase64File(fileContent))) {
                throw ConstraintViolationException(
                    setOf(
                        DefaultConstraintViolation(
                            "dokumentasjon",
                            constraint = VirusCheckConstraint()
                        )
                    )
                )
            }
            bucket.uploadDoc(id, fileContent, fileExt)
        }
    }
}
