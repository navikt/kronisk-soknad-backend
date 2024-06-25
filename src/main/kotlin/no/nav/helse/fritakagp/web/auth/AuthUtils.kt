package no.nav.helse.fritakagp.web.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.ApplicationRequest
import io.ktor.util.pipeline.PipelineContext
import no.nav.helse.fritakagp.integration.altinn.ManglerAltinnRettigheterException
import no.nav.helse.fritakagp.integration.altinn.hasAccess
import no.nav.helsearbeidsgiver.altinn.AltinnClient
import no.nav.security.token.support.core.jwt.JwtToken
import java.time.Instant
import java.util.Date

fun PipelineContext<Unit, ApplicationCall>.authorize(authorizer: AltinnClient, arbeidsgiverId: String) {
    val identitetsnummer = hentIdentitetsnummerFraLoginToken(call.request)
    if (!authorizer.hasAccess(identitetsnummer, arbeidsgiverId)) {
        throw ManglerAltinnRettigheterException()
    }
}

fun hentIdentitetsnummerFraLoginToken(request: ApplicationRequest): String {
    val tokenString = getTokenString(request)
    val pid = JwtToken(tokenString).jwtTokenClaims.get("pid")
    return pid?.toString() ?: JwtToken(tokenString).subject
}

fun hentUtløpsdatoFraLoginToken(request: ApplicationRequest): Date {
    val tokenString = getTokenString(request)
    return JwtToken(tokenString).jwtTokenClaims.expirationTime ?: Date.from(Instant.MIN)
}

private fun getTokenString(request: ApplicationRequest): String {
    return request.headers["Authorization"]?.replaceFirst("Bearer ", "")
        ?: throw IllegalAccessException("Du må angi et identitetstoken i Authorization-headeren")
}
