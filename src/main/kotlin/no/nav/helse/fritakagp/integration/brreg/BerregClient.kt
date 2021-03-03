package no.nav.helse.fritakagp.integration.brreg

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*

interface BerregClient {
    suspend fun getVirksomhetsNavn(orgnr : String) : String
}

class MockBerregClient : BerregClient {
    override suspend fun getVirksomhetsNavn(orgnr: String): String {
       return "Stark Industries"
    }
}

class BerregClientImp(private val httpClient: HttpClient, private val om : ObjectMapper, private val berreUrl : String) : BerregClient {
    override suspend fun getVirksomhetsNavn(orgnr: String): String {
        return httpClient.get<BerregResponse> (
            url {
                protocol = URLProtocol.HTTPS
                host = berreUrl
                path(orgnr)
            }).navn
    }
}


