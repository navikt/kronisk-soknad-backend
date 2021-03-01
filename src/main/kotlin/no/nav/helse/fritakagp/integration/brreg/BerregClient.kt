package no.nav.helse.fritakagp.integration.brreg

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import no.nav.helse.fritakagp.web.dto.validation.EnhetsregisteretConstraint
import org.valiktor.ConstraintViolationException
import org.valiktor.DefaultConstraintViolation

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
        val response = httpClient.get<HttpResponse> (
            url {
                protocol = URLProtocol.HTTPS
                host = berreUrl
                path(orgnr)
            })

        return if (response.status != HttpStatusCode.OK)
            throw ConstraintViolationException(
                setOf(
                    DefaultConstraintViolation(
                        "virkomhetsnummer",
                        constraint = EnhetsregisteretConstraint()
                    )
                ))
            else
                om.readValue(response.readText(),BerregResponse::class.java).navn
    }
}


