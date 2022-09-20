package no.nav.helse.fritakagp.service

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.runBlocking
import no.nav.helse.fritakagp.web.auth.hentIdentitetsnummerFraLoginToken
import no.nav.helsearbeidsgiver.altinn.AltinnClient

class AltinnService(
    private val altinnClient: AltinnClient,
) {
    fun authorize(context: PipelineContext<Unit, ApplicationCall>, arbeidsgiverId: String) {
        val identitetsnummer = context.hentIdentitetsnummerFraLoginToken(context.call.request)

        val harRettighet = runBlocking {
            altinnClient.harRettighetForOrganisasjon(
                identitetsnummer = identitetsnummer,
                organisasjonId = arbeidsgiverId,
            )
        }

        if (!harRettighet) {
            throw ManglerAltinnRettigheterException()
        }
    }
}

class ManglerAltinnRettigheterException : Exception()
