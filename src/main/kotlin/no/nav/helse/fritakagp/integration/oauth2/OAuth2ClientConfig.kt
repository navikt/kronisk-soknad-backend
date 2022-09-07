package no.nav.helse.fritakagp.integration.oauth2

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.tryGetString
import no.nav.helse.fritakagp.config.prop
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import java.net.URI

class OAuth2ClientConfig(
    appConfig: ApplicationConfig,
    scope: String
) {
    internal val clientConfig: Map<String, ClientProperties> =
        appConfig.configList(CLIENTS_PATH)
            .associate { config ->
                val wellKnownUrl = config.tryGetString("well_known_url")
                val resourceUrl = config.tryGetString("resource_url")
                config.prop(CLIENT_NAME) to ClientProperties(
                    URI(config.prop("token_endpoint_url")),
                    wellKnownUrl?.let { URI(it) },
                    OAuth2GrantType(config.prop("grant_type")),
                    config.tryGetString(scope)?.split(","),
                    ClientAuthenticationProperties(
                        config.prop("authentication.client_id"),
                        ClientAuthenticationMethod(
                            config.prop("authentication.client_auth_method")
                        ),
                        config.tryGetString("authentication.client_secret"),
                        config.tryGetString("authentication.client_jwk")
                    ),
                    resourceUrl?.let { URI(it) },
                    ClientProperties.TokenExchangeProperties(
                        config.tryGetString("token-exchange.audience") ?: "",
                        config.tryGetString("token-exchange.resource")
                    )
                )
            }

    companion object CommonConfigurationAttributes {
        const val COMMON_PREFIX = "no.nav.security.jwt.client.registration"
        const val CLIENTS_PATH = "$COMMON_PREFIX.clients"
        const val CLIENT_NAME = "client_name"
    }
}
