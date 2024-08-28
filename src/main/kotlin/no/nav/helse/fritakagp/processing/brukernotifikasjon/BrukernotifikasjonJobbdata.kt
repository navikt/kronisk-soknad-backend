package no.nav.helse.fritakagp.processing.brukernotifikasjon

import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonJobbdata.NotifikasjonsType.Annullere
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonJobbdata.NotifikasjonsType.Endre
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonJobbdata.NotifikasjonsType.Opprette
import java.util.UUID

private const val ENDEPUNKT_KRONISK = "/nb/notifikasjon/kronisk/"

private const val ENDEPUNKT_GRAVID = "/nb/notifikasjon/gravid/"

data class BrukernotifikasjonJobbdata(
    val skjemaId: UUID,
    val skjemaType: SkjemaType,
    val notifikasjonsType: NotifikasjonsType = Opprette
) {
    fun getTekst(virksomhetsnavn: String?): String {
        val ukjentArbeidsgiver = "Arbeidsgiveren din"
        return when (notifikasjonsType) {
            Opprette -> "${virksomhetsnavn ?: ukjentArbeidsgiver} har søkt om at NAV dekker sykepenger fra første dag av sykefraværet ditt."
            Endre -> "${virksomhetsnavn ?: ukjentArbeidsgiver} har søkt om at NAV dekker sykepenger fra første dag av sykefraværet ditt."
            Annullere -> "${virksomhetsnavn ?: ukjentArbeidsgiver} har trukket kravet om at NAV dekker sykepenger fra første dag av sykefraværet ditt."
        }
    }

    fun getLenke(): String {
        when (skjemaType) {
            SkjemaType.KroniskKrav -> {
                return when (notifikasjonsType) {
                    Opprette -> "${ENDEPUNKT_KRONISK}krav/$skjemaId"
                    Endre -> "${ENDEPUNKT_KRONISK}krav/$skjemaId"
                    Annullere -> "${ENDEPUNKT_KRONISK}krav/slettet/$skjemaId"
                }
            }

            SkjemaType.KroniskSøknad -> {
                return when (notifikasjonsType) {
                    Opprette -> "${ENDEPUNKT_KRONISK}soknad/$skjemaId"
                    Endre -> "${ENDEPUNKT_KRONISK}soknad/$skjemaId"
                    Annullere -> "${ENDEPUNKT_KRONISK}soknad/$skjemaId"
                }
            }

            SkjemaType.GravidKrav -> {
                return when (notifikasjonsType) {
                    Opprette -> "${ENDEPUNKT_GRAVID}krav/$skjemaId"
                    Endre -> "${ENDEPUNKT_GRAVID}krav/$skjemaId"
                    Annullere -> "${ENDEPUNKT_GRAVID}krav/slettet/$skjemaId"
                }
            }

            SkjemaType.GravidSøknad -> {
                return when (notifikasjonsType) {
                    Opprette -> "${ENDEPUNKT_GRAVID}soknad/$skjemaId"
                    Endre -> "${ENDEPUNKT_GRAVID}soknad/$skjemaId"
                    Annullere -> "${ENDEPUNKT_GRAVID}soknad/$skjemaId"
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
        Opprette,
        Endre,
        Annullere
    }
}
