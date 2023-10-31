package no.nav.helse.fritakagp.integration.brreg

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

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
        var navn: String? = "Ukjent arbeidsgiver"
        val httpResponse: HttpResponse = httpResponse(orgnr)
        if (httpResponse.status.isSuccess()) {
            val enhet: UnderenheterResponse = httpResponse.body()
            navn = enhet.navn
        } else {
            if (404 == httpResponse.status?.value) {
                return navn
            }
            throw ClientRequestException(httpResponse, "Feil ved kall til brreg")
        }
        return navn
    }

    private suspend fun httpResponse(orgnr: String): HttpResponse {
        val url = "${brregUndervirksomhetUrl.trimEnd('/')}/$orgnr"
        val httpResponse: HttpResponse = httpClient.get(url) {
            contentType(ContentType.Application.Json)
        }
        return httpResponse
    }

    // Sjekker at vi har en virksomhet som eksisterer og ikke er slettet.
    override suspend fun erVirksomhet(orgNr: String): Boolean {
        val httpResponse: HttpResponse = httpResponse(orgNr)
        if (httpResponse.status.isSuccess()) {
            val enhet: UnderenheterResponse = httpResponse.body()
            val slettedato = enhet.sletteDato
            return (slettedato == null)
        } else {
            if (404 == httpResponse.status?.value) {
                return false
            }
            throw ClientRequestException(httpResponse, "Feil ved kall til brreg")
        }
    }
}
