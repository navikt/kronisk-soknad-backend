package no.nav.helse.fritakagp.integration.virusscan

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.core.*


interface VirusScanner {
    suspend fun scanDoc(vedlagt : ByteArray) : Boolean
}

class MockVirusScanner : VirusScanner {
    override suspend fun scanDoc(vedlagt: ByteArray): Boolean {
        return true
    }
}


class ClamavVirusScannerImp(private val httpClient: HttpClient, private val scanUrl : String) : VirusScanner {
    data class ScanResult(
        val Filename: String,
        val Result: Result
    )
    enum class Result {
        FOUND, OK, ERROR
    }
    override suspend fun scanDoc(vedlagt: ByteArray): Boolean {
        val scanResult = httpClient.request<List<ScanResult>> {
            url(scanUrl)
            method = HttpMethod.Post
            body = MultiPartFormDataContent(
                    formData {
                        append("file1",
                                "vedlagt",
                                ContentType.parse("application/octet-stream"),
                                vedlagt.size.toLong()
                        ) {
                            writeFully(vedlagt)
                        }
                    })
        }

        return when(scanResult[0].Result) {
            Result.OK -> true
            Result.FOUND, Result.ERROR -> false
        }
    }
}
