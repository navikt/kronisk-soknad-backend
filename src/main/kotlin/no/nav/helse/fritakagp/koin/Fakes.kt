package no.nav.helse.fritakagp.koin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.*
import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnOrganisasjon
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostRequest
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.*
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.*
import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.helse.arbeidsgiver.web.auth.AltinnOrganisationsRepository
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.integration.brreg.MockBrregClient
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.integration.gcp.MockBucketStorage
import no.nav.helse.fritakagp.integration.kafka.BrukernotifikasjonBeskjedSender
import no.nav.helse.fritakagp.integration.kafka.MockBrukernotifikasjonBeskjedSender
import no.nav.helse.fritakagp.integration.virusscan.MockVirusScanner
import no.nav.helse.fritakagp.integration.virusscan.VirusScanner
import org.koin.core.module.Module
import org.koin.dsl.bind
import java.time.LocalDate
import java.time.LocalDateTime

fun Module.mockExternalDependecies() {
    single { MockAltinnRepo(get()) } bind AltinnOrganisationsRepository::class
    single { MockBrukernotifikasjonBeskjedSender() } bind BrukernotifikasjonBeskjedSender::class

    single {
        object : AaregArbeidsforholdClient {
            override suspend fun hentArbeidsforhold(ident: String, callId: String): List<Arbeidsforhold> =
                listOf<Arbeidsforhold>(
                    Arbeidsforhold(
                        Arbeidsgiver("test", "810007842"), Opplysningspliktig("Juice", "810007702"), emptyList(),
                        Ansettelsesperiode(
                            Periode(LocalDate.MIN, null)
                        ),
                        LocalDate.MIN.atStartOfDay()
                    ),
                    Arbeidsforhold(
                        Arbeidsgiver("test", "910098896"), Opplysningspliktig("Juice", "910098896"), emptyList(),
                        Ansettelsesperiode(
                            Periode(
                                LocalDate.MIN, null
                            )
                        ),
                        LocalDate.MIN.atStartOfDay()
                    ),
                    Arbeidsforhold(
                        Arbeidsgiver("test", "917404437"), Opplysningspliktig("Juice", "910098896"), emptyList(),
                        Ansettelsesperiode(
                            Periode(
                                LocalDate.MIN, null
                            )
                        ),
                        LocalDate.MIN.atStartOfDay()
                    )
                )
        }
    } bind AaregArbeidsforholdClient::class

    single {
        object : DokarkivKlient {
            override fun journalførDokument(
                journalpost: JournalpostRequest,
                forsoekFerdigstill: Boolean,
                callId: String
            ): JournalpostResponse {
                return JournalpostResponse("arkiv-ref", true, "J", null, emptyList())
            }
        }
    } bind DokarkivKlient::class

    single {
        object : PdlClient {
            override fun fullPerson(ident: String) =
                PdlHentFullPerson(
                    PdlHentFullPerson.PdlFullPersonliste(
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList()
                    ),

                    PdlHentFullPerson.PdlIdentResponse(listOf(PdlIdent("aktør-id", PdlIdent.PdlIdentGruppe.AKTORID))),

                    PdlHentFullPerson.PdlGeografiskTilknytning(
                        PdlHentFullPerson.PdlGeografiskTilknytning.PdlGtType.UTLAND,
                        null,
                        null,
                        "SWE"
                    )
                )

            override fun personNavn(ident: String) =
                PdlHentPersonNavn.PdlPersonNavneliste(
                    listOf(
                        PdlHentPersonNavn.PdlPersonNavneliste.PdlPersonNavn(
                            "Ola",
                            "M",
                            "Avsender",
                            PdlPersonNavnMetadata("freg")
                        )
                    )
                )
        }
    } bind PdlClient::class

    single {
        object : OppgaveKlient {
            override suspend fun hentOppgave(oppgaveId: Int, callId: String): OppgaveResponse {
                return OppgaveResponse(oppgaveId, 1, oppgavetype = "JFR", aktivDato = LocalDateTime.now().minusDays(3).toLocalDate(), prioritet = Prioritet.NORM.toString())
            }

            override suspend fun opprettOppgave(
                opprettOppgaveRequest: OpprettOppgaveRequest,
                callId: String
            ): OpprettOppgaveResponse = OpprettOppgaveResponse(
                1234, "0100",
                tema = "KON",
                oppgavetype = "JFR",
                versjon = 1,
                aktivDato = LocalDate.now(),
                Prioritet.NORM,
                Status.UNDER_BEHANDLING
            )
        }
    } bind OppgaveKlient::class

    single { MockVirusScanner() } bind VirusScanner::class
    single { MockBucketStorage() } bind BucketStorage::class
    single { MockBrregClient() } bind BrregClient::class
}

class MockAltinnRepo(om: ObjectMapper) : AltinnOrganisationsRepository {
    private val mockList = "altinn-mock/organisasjoner-med-rettighet.json".loadFromResources()
    private val mockAcl = om.readValue<Set<AltinnOrganisasjon>>(mockList)
    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> = mockAcl
}
