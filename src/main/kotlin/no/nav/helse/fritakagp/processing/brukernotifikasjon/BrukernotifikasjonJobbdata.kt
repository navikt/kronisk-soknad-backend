package no.nav.helse.fritakagp.processing.brukernotifikasjon

import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonJobbdata.NotifikasjonsType.Annullere
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonJobbdata.NotifikasjonsType.Endre
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonJobbdata.NotifikasjonsType.Opprette
import java.util.UUID

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
                    Opprette -> "/nb/notifikasjon/kronisk/krav/$skjemaId"
                    Endre -> "/nb/notifikasjon/kronisk/krav/$skjemaId"
                    Annullere -> "/nb/notifikasjon/kronisk/krav/slettet/$skjemaId"
                }
            }

            SkjemaType.KroniskSøknad -> {
                return when (notifikasjonsType) {
                    Opprette -> "/nb/notifikasjon/kronisk/soknad/$skjemaId"
                    Endre -> "/nb/notifikasjon/kronisk/soknad/$skjemaId"
                    Annullere -> "/nb/notifikasjon/kronisk/soknad/$skjemaId"
                }
            }

            SkjemaType.GravidKrav -> {
                return when (notifikasjonsType) {
                    Opprette -> "/nb/notifikasjon/gravid/krav/$skjemaId"
                    Endre -> "/nb/notifikasjon/gravid/krav/$skjemaId"
                    Annullere -> "/nb/notifikasjon/gravid/krav/slettet/$skjemaId"
                }
            }

            SkjemaType.GravidSøknad -> {
                return when (notifikasjonsType) {
                    Opprette -> "/nb/notifikasjon/gravid/soknad/$skjemaId"
                    Endre -> "/nb/notifikasjon/gravid/soknad/$skjemaId"
                    Annullere -> "/nb/notifikasjon/gravid/soknad/$skjemaId"
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
