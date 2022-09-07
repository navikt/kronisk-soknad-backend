package no.nav.helse.fritakagp.web.auth

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.config.ApplicationConfig
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import no.nav.helse.arbeidsgiver.system.AppEnv
import no.nav.helse.arbeidsgiver.system.getEnvironment
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.security.mock.oauth2.MockOAuth2Server

fun Application.localCookieDispenser(config: ApplicationConfig) {
    val oauthMockPort = 6666
    val logger = "LocalCookieDispenser".logger()
    logger.info("Starter OAuth Mock")
    val server = MockOAuth2Server()
    val cookieName = config.configList("no.nav.security.jwt.issuers")[0].property("cookie_name").getString()
    val issuerName = config.configList("no.nav.security.jwt.issuers")[0].property("issuer_name").getString()
    val audience = config.configList("no.nav.security.jwt.issuers")[0].property("accepted_audience").getString()
    val domain = if (config.getEnvironment() == AppEnv.PREPROD) "dev.nav.no" else "localhost"

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
