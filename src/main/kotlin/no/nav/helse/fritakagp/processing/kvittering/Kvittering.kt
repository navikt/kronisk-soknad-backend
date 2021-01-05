package no.nav.helse.fritakagp.processing.kvittering

import java.time.LocalDateTime
import java.util.*

data class Kvittering(
        val id: UUID = UUID.randomUUID(),
        val virksomhetsnummer: String,
        val tidspunkt: LocalDateTime,
        var status: KvitteringStatus = KvitteringStatus.OPPRETTET
)

enum class KvitteringStatus {
    OPPRETTET,
    SENDT,
    FEILET,
    JOBB
}