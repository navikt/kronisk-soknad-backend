package no.nav.helse.fritakagp.web.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.application
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.authorization
import io.ktor.util.pipeline.PipelineContext
import no.nav.helse.fritakagp.config.jwtIssuerCookieName
import no.nav.security.token.support.core.jwt.JwtToken
import java.time.Instant
import java.util.Date

fun PipelineContext<Unit, ApplicationCall>.hentIdentitetsnummerFraLoginToken(request: ApplicationRequest): String =
    getTokenString(request)
        .let {
            it.jwtTokenClaims
                .get("pid")
                ?.toString()
                ?: it.subject
        }

fun PipelineContext<Unit, ApplicationCall>.hentUtløpsdatoFraLoginToken(request: ApplicationRequest): Date =
    getTokenString(request)
        .jwtTokenClaims
        .expirationTime
        ?: Date.from(Instant.MIN)

private fun PipelineContext<Unit, ApplicationCall>.getTokenString(request: ApplicationRequest): JwtToken =
    listOfNotNull(
        request.authorization()?.removePrefix("Bearer "),
        request.cookies[application.environment.config.jwtIssuerCookieName()]
    )
        .firstOrNull()
        ?.let(::JwtToken)
        ?: throw IllegalAccessException("Du må angi et identitetstoken som i cookie eller i Authorization-headeren")
