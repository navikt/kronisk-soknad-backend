package no.nav.helse.fritakagp.integration.brreg

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*

interface BrregClient {
    suspend fun getVirksomhetsNavn(orgnr: String): String?
    suspend fun erVirksomhet(orgNr: String): Boolean
}

class MockBrregClient : BrregClient {
    override suspend fun getVirksomhetsNavn(orgnr: String): String? {
        return "Stark Industries"
    }

    override suspend fun erVirksomhet(orgNr: String): Boolean {
        return true
    }
}

class BrregClientImpl(private val httpClient: HttpClient, private val brregUndervirksomhetUrl: String) : BrregClient {
    override suspend fun getVirksomhetsNavn(orgnr: String): String? {
        var navn: String? = null
        try {
            val url = "${brregUndervirksomhetUrl.trimEnd('/')}/$orgnr"
            navn = httpClient.get<UnderenheterNavnResponse>(url).navn
        } catch (cause: Throwable) {
            when (cause) {
                is ClientRequestException -> {
                    if (404 == cause.response?.status?.value)
                        navn = "Ukjent arbeidsgiver"
                }
                else -> throw cause
            }
        }

        return navn
    }

    override suspend fun erVirksomhet(orgNr: String): Boolean {
        return try {
            val url = "${brregUndervirksomhetUrl.trimEnd('/')}/$orgNr"
            httpClient.get<Unit>(url)
            true
        } catch (cause: ClientRequestException) {
            if (404 == cause.response?.status?.value) {
                false
            } else {
                throw cause
            }
        }
    }
}
