package no.nav.helse.arbeidsgiver.integrasjoner.aareg2

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.http.headers
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider

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
        val stsToken = stsClient.getToken()
        return httpClient.get(url) {
            bearerAuth(stsToken)
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json)
                append("X-Correlation-ID", callId)
                append("Nav-Consumer-Token", "Bearer $stsToken")
                append("Nav-Personident", ident)
            }
        }.body()
    }
}
