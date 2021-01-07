package no.nav.helse.fritakagp.domain

import java.util.*

fun decodeBase64File(datafile: String): ByteArray {
    return Base64.getDecoder().decode(datafile)
}

enum class GodskjentFiletyper(val beskrivelse : String) {
    PDF("pdf"),
    JPEG("jpg"),
    PNG("png")
}