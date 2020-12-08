package no.nav.helse.fritakagp.virusscan

import com.fasterxml.jackson.annotation.JsonAlias
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import org.koin.core.KoinComponent
import org.koin.core.inject


interface VirusScanner {
    suspend fun scanDoc(vedlagt : ByteArray) : Boolean
}
data class ScanResult(
    val filename: String,
    val result: Result
)
enum class Result {
    FOUND, OK, ERROR
}
class MockVirusScanner : VirusScanner {
    override suspend fun scanDoc(vedlagt: ByteArray): Boolean {
        return true
    }
}


class ClamavVirusScannerImp(private val httpClient: HttpClient, private val scanUrl : String) : VirusScanner {
    override suspend fun scanDoc(vedlagt: ByteArray): Boolean {
        val scanResult = httpClient.request<ScanResult> {
            url(scanUrl)
            method = HttpMethod.Post
            headers {
                append("Content-Type", "multipart/form-data")
            }
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

        return when(scanResult.result) {
            Result.OK -> true
            Result.FOUND, Result.ERROR -> false
        }
    }
}

class ClamavVirusScanner : KoinComponent {
    val clamavService by inject<VirusScanner>()
    suspend fun scanFile(vedlagt: ByteArray) = clamavService.scanDoc(vedlagt)
}