package no.nav.helse.fritakagp.integration.brreg

data class BerregResponse(
    val _links: Links,
    val antallAnsatte: Int,
    val forretningsadresse: Forretningsadresse,
    val hjemmeside: String,
    val institusjonellSektorkode: InstitusjonellSektorkode,
    val konkurs: Boolean,
    val maalform: String,
    val naeringskode1: Naeringskode1,
    val navn: String,
    val organisasjonsform: Organisasjonsform,
    val organisasjonsnummer: String,
    val overordnetEnhet: String,
    val postadresse: Postadresse,
    val registreringsdatoEnhetsregisteret: String,
    val registrertIForetaksregisteret: Boolean,
    val registrertIFrivillighetsregisteret: Boolean,
    val registrertIMvaregisteret: Boolean,
    val registrertIStiftelsesregisteret: Boolean,
    val underAvvikling: Boolean,
    val underTvangsavviklingEllerTvangsopplosning: Boolean
)

data class Links(
    val overordnetEnhet: OverordnetEnhet,
    val self: Self
)

data class Forretningsadresse(
    val adresse: List<String>,
    val kommune: String,
    val kommunenummer: String,
    val land: String,
    val landkode: String,
    val postnummer: String,
    val poststed: String
)

data class InstitusjonellSektorkode(
    val beskrivelse: String,
    val kode: String
)

data class Naeringskode1(
    val beskrivelse: String,
    val kode: String
)

data class Organisasjonsform(
    val _links: LinksX,
    val beskrivelse: String,
    val kode: String
)

data class Postadresse(
    val adresse: List<String>,
    val kommune: String,
    val kommunenummer: String,
    val land: String,
    val landkode: String,
    val postnummer: String,
    val poststed: String
)

data class OverordnetEnhet(
    val href: String
)

data class Self(
    val href: String
)

data class LinksX(
    val self: SelfX
)

data class SelfX(
    val href: String
)
