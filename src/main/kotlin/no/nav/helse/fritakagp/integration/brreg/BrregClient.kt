package no.nav.helse.fritakagp.integration.brreg

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get

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
            val underenhetResponse: UnderenheterResponse = httpClient.get(url).body()
            val navn = underenhetResponse.navn
        } catch (cause: Throwable) {
            when (cause) {
                is ClientRequestException -> {
                    if (404 == cause.response?.status?.value) {
                        navn = "Ukjent arbeidsgiver"
                    }
                }
                else -> throw cause
            }
        }
        return navn
    }

    // Sjekker at vi har en virksomhet som eksisterer og ikke er slettet.
    override suspend fun erVirksomhet(orgNr: String): Boolean {
        return try {
            val url = "${brregUndervirksomhetUrl.trimEnd('/')}/$orgNr"
            val underenheterResponse: UnderenheterResponse = httpClient.get(url).body()
            val slettedato = underenheterResponse.sletteDato
            return (slettedato == null)
        } catch (cause: ClientRequestException) {
            if (404 == cause.response?.status?.value) {
                false
            } else {
                throw cause
            }
        }
    }
}
