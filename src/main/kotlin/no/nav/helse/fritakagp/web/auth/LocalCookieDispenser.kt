package no.nav.helse.fritakagp.web.auth

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import no.nav.helse.fritakagp.EnvJwt
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.security.mock.oauth2.MockOAuth2Server

private val logger = "LocalCookieDispenser".logger()

fun Application.localCookieDispenser(env: EnvJwt, domain: String) {
    logger.info("Starter OAuth2-mock")

    val server = MockOAuth2Server().apply { start(port = 6666) }

    logger.info("Startet OAuth2-mock på ${server.jwksUrl(env.issuerName)}")

    routing {
        get("/local/cookie-please") {
            val token = server.issueToken(
                subject = call.request.queryParameters["subject"].toString(),
                issuerId = env.issuerName,
                audience = env.audience
            )

            val cookie = Cookie(
                name = env.cookieName,
                value = token.serialize(),
                encoding = CookieEncoding.RAW,
                domain = domain,
                path = "/"
            )

            call.response.cookies.append(cookie)

            if (call.request.queryParameters["redirect"] != null) {
                call.respondText("<script>window.location.href='" + call.request.queryParameters["redirect"] + "';</script>", ContentType.Text.Html, HttpStatusCode.OK)
            } else {
                call.respondText("Cookie Set", ContentType.Text.Plain, HttpStatusCode.OK)
            }
        }
    }
}
