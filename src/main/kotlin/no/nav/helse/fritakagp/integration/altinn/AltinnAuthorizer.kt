package no.nav.helse.fritakagp.integration.altinn


interface AltinnAuthorizer {
    /**
     * MB: Kopiert og inlinet fra hag-felles-backend
     * Sjekker om den angitte identiteten har rettighet til å se refusjoner for den angitte arbeidsgiverId
     * En arbeidsgiverId kan være en virksomhet, en hovedenhet, et identitetsnummer på en privatperson eller et
     * organisasjonsledd.
     */
    fun hasAccess(identitetsnummer: String, arbeidsgiverId: String): Boolean

}

class DefaultAltinnAuthorizer(private val authListRepo: AltinnRepo) : AltinnAuthorizer {
    override fun hasAccess(identitetsnummer: String, arbeidsgiverId: String): Boolean {
        return authListRepo.hentOrgMedRettigheterForPerson(identitetsnummer)
            .any {
                it.organizationNumber == arbeidsgiverId && it.parentOrganizationNumber != null
            }
    }
}

//class HentEgneOrgnrAltinnAuthorizer(private val authListRepo: AltinnRepo) : AltinnAuthorizer {
//    override fun hasAccess(identitetsnummer: String, arbeidsgiverId: String): Boolean {
//        return authListRepo.hentOrgMedRettigheterForPerson(identitetsnummer).size > 0
//    }
//}
