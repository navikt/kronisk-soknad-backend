package no.nav.helse.fritakagp.web.auth

import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.helse.fritakagp.config.AppEnv
import no.nav.helse.fritakagp.config.env
import no.nav.helse.fritakagp.config.jwtIssuerAudience
import no.nav.helse.fritakagp.config.jwtIssuerCookieName
import no.nav.helse.fritakagp.config.jwtIssuerName
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.LoggerFactory

fun Application.localCookieDispenser(config: ApplicationConfig) {
    val oauthMockPort = 6666
    val logger = LoggerFactory.getLogger("LocalCookieDispenser")
    logger.info("Starter OAuth Mock")
    val server = MockOAuth2Server()
    val cookieName = config.jwtIssuerCookieName()
    val issuerName = config.jwtIssuerName()
    val audience = config.jwtIssuerAudience()
    val domain = if (config.env() == AppEnv.PREPROD) "dev.nav.no" else "localhost"

    server.start(port = oauthMockPort)
    logger.info("Startet OAuth mock på ${server.jwksUrl(issuerName)}")

    routing {
        get("/local/cookie-please") {
            val token = server.issueToken(
                subject = call.request.queryParameters["subject"].toString(),
                issuerId = issuerName,
                audience = audience
            )

            call.response.cookies.append(Cookie(cookieName, token.serialize(), CookieEncoding.RAW, domain = domain, path = "/"))

            if (call.request.queryParameters["redirect"] != null) {
                call.respondText("<script>window.location.href='" + call.request.queryParameters["redirect"] + "';</script>", ContentType.Text.Html, HttpStatusCode.OK)
            } else {
                call.respondText("Cookie Set", ContentType.Text.Plain, HttpStatusCode.OK)
            }
        }
    }
}
