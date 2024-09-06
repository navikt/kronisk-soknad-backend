package no.nav.helse.fritakagp.processing.brukernotifikasjon

import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonJobbdata.NotifikasjonsType.Annullering
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonJobbdata.NotifikasjonsType.Endring
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonJobbdata.NotifikasjonsType.Oppretting
import java.util.UUID

private const val ENDEPUNKT_KRONISK = "/nb/notifikasjon/kronisk/"

private const val ENDEPUNKT_GRAVID = "/nb/notifikasjon/gravid/"

data class BrukernotifikasjonJobbdata(
    val skjemaId: UUID,
    val identitetsnummer: String,
    val virksomhetsnavn: String?,
    val skjemaType: SkjemaType,
    val notifikasjonsType: NotifikasjonsType = Oppretting
) {
    fun hentTekst(): String {
        val ukjentArbeidsgiver = "Arbeidsgiveren din"
        return when (notifikasjonsType) {
            Oppretting -> "${virksomhetsnavn ?: ukjentArbeidsgiver} har søkt om at NAV dekker sykepenger fra første dag av sykefraværet ditt."
            Endring -> "${virksomhetsnavn ?: ukjentArbeidsgiver} har søkt om at NAV dekker sykepenger fra første dag av sykefraværet ditt."
            Annullering -> "${virksomhetsnavn ?: ukjentArbeidsgiver} har trukket kravet om at NAV dekker sykepenger fra første dag av sykefraværet ditt."
        }
    }

    fun hentLenke(): String {
        when (skjemaType) {
            SkjemaType.KroniskKrav -> {
                return when (notifikasjonsType) {
                    Oppretting -> "${ENDEPUNKT_KRONISK}krav/$skjemaId"
                    Endring -> "${ENDEPUNKT_KRONISK}krav/$skjemaId"
                    Annullering -> "${ENDEPUNKT_KRONISK}krav/slettet/$skjemaId"
                }
            }

            SkjemaType.KroniskSøknad -> {
                return when (notifikasjonsType) {
                    Oppretting -> "${ENDEPUNKT_KRONISK}soknad/$skjemaId"
                    Endring -> "${ENDEPUNKT_KRONISK}soknad/$skjemaId"
                    Annullering -> "${ENDEPUNKT_KRONISK}soknad/$skjemaId"
                }
            }

            SkjemaType.GravidKrav -> {
                return when (notifikasjonsType) {
                    Oppretting -> "${ENDEPUNKT_GRAVID}krav/$skjemaId"
                    Endring -> "${ENDEPUNKT_GRAVID}krav/$skjemaId"
                    Annullering -> "${ENDEPUNKT_GRAVID}krav/slettet/$skjemaId"
                }
            }

            SkjemaType.GravidSøknad -> {
                return when (notifikasjonsType) {
                    Oppretting -> "${ENDEPUNKT_GRAVID}soknad/$skjemaId"
                    Endring -> "${ENDEPUNKT_GRAVID}soknad/$skjemaId"
                    Annullering -> "${ENDEPUNKT_GRAVID}soknad/$skjemaId"
                }
            }
        }
    }

    enum class SkjemaType {
        KroniskKrav,
        KroniskSøknad,
        GravidKrav,
        GravidSøknad
    }

    enum class NotifikasjonsType {
        Oppretting,
        Endring,
        Annullering
    }
}
