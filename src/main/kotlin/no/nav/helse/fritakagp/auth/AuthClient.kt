package no.nav.helse.fritakagp.auth

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.forms.submitForm
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlinx.coroutines.runBlocking
import no.nav.helse.fritakagp.Env
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

enum class IdentityProvider(@JsonValue val alias: String) {
    MASKINPORTEN("maskinporten"),
    AZURE_AD("azuread"),
    IDPORTEN("idporten"),
    TOKEN_X("tokenx")
}

sealed class TokenResponse {
    data class Success(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("expires_in")
        val expiresInSeconds: Int
    ) : TokenResponse()

    data class Error(
        val error: TokenErrorResponse,
        val status: HttpStatusCode
    ) : TokenResponse()
}

data class TokenErrorResponse(
    val error: String,
    @JsonProperty("error_description")
    val errorDescription: String
)

data class TokenIntrospectionResponse(
    val active: Boolean,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val error: String?,
    @JsonAnySetter @get:JsonAnyGetter
    val other: Map<String, Any?> = mutableMapOf()
)

class AuthClient(
    private val env: Env,
    private val httpClient: HttpClient,
    private val provider: IdentityProvider
) {

    suspend fun token(target: String): TokenResponse = try {
        sikkerLogger().info("Requesting token for $target from ${provider.alias} and endpoint ${env.tokenEndpoint}")
        httpClient.submitForm(
            env.tokenEndpoint,
            parameters {
                set("target", target)
                set("identity_provider", provider.alias)
            }
        ).body<TokenResponse.Success>()
    } catch (e: ResponseException) {
        TokenResponse.Error(e.response.body<TokenErrorResponse>(), e.response.status)
    }

    suspend fun exchange(target: String, userToken: String, provider: IdentityProvider): TokenResponse = try {
        httpClient.submitForm(
            env.tokenExchangeEndpoint,
            parameters {
                set("target", target)
                set("user_token", userToken)
                set("identity_provider", provider.alias)
            }
        ).body<TokenResponse.Success>()
    } catch (e: ResponseException) {
        TokenResponse.Error(e.response.body<TokenErrorResponse>(), e.response.status)
    }

    suspend fun introspect(accessToken: String, provider: IdentityProvider): TokenIntrospectionResponse =
        httpClient.submitForm(
            env.tokenIntrospectionEndpoint,
            parameters {
                set("token", accessToken)
                set("identity_provider", provider.alias)
            }
        ).body()
}

fun getFetchToken(authClient: AuthClient, target: String): () -> String = {
    runBlocking {
        authClient.token(target).let {
            when (it) {
                is TokenResponse.Success -> it.accessToken
                is TokenResponse.Error -> {
                    sikkerLogger().error("Failed to fetch token status: ${it.status} - ${it.error.errorDescription}")
                    throw RuntimeException("Failed to fetch token status: ${it.status} - ${it.error.errorDescription}")
                }
            }
        }
    }
}
