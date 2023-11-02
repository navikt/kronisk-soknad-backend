package no.nav.helse.arbeidsgiver.integrasjoner.aareg2

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.helsearbeidsgiver.tokenprovider.AccessTokenProvider

interface AaregArbeidsforholdClient {
    suspend fun hentArbeidsforhold(ident: String, callId: String): List<Arbeidsforhold>
}

/**
 * klient for å hente ut aktive arbeidsforhold på en person
 */
class AaregArbeidsforholdClientImpl(
    private val url: String,
    private val stsClient: AccessTokenProvider,
    private val httpClient: HttpClient
) : AaregArbeidsforholdClient {

    override suspend fun hentArbeidsforhold(ident: String, callId: String): List<Arbeidsforhold> {
        val token = stsClient.getToken()
        return try {
            val payload = httpClient.get(url) {
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                header("X-Correlation-ID", callId)
                header("Nav-Consumer-Token", "Bearer $token")
                header("Nav-Personident", ident)
            }.body<List<Arbeidsforhold>>()
            payload
        } catch (e: Exception) {
            emptyList()
        }
    }
}
