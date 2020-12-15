package no.nav.helse.fritakagp.oauth2

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.security.token.support.client.core.context.JwtBearerTokenResolver
import no.nav.security.token.support.client.core.http.OAuth2HttpClient
import no.nav.security.token.support.client.core.http.OAuth2HttpRequest
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal
import java.util.*


class DefaultOAuth2HttpClient(private val httpClient: HttpClient) : OAuth2HttpClient {
    override fun post(oAuth2HttpRequest: OAuth2HttpRequest): OAuth2AccessTokenResponse {
        return runBlocking {
            httpClient.submitForm(
                url = oAuth2HttpRequest.tokenEndpointUrl.toString(),
                formParameters = Parameters.build {
                    oAuth2HttpRequest.formParameters.forEach {
                        append(it.key, it.value)
                    }
                }
            )
        }
    }
}

class TokenResolver: JwtBearerTokenResolver {
    var tokenPrincipal: TokenValidationContextPrincipal? = null

    override fun token(): Optional<String> {
        return tokenPrincipal?.context?.firstValidToken?.map { it.tokenAsString } ?: Optional.empty()
    }
}