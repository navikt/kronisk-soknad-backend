package no.nav.helse.fritakagp.koin

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.AaregArbeidsforholdClient
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Ansettelsesperiode
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsgiver
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Opplysningspliktig
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Periode
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostRequest
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.Prioritet
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.Status
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentPersonNavn
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlIdent
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlPersonNavnMetadata
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.integration.brreg.MockBrregClient
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.integration.gcp.MockBucketStorage
import no.nav.helse.fritakagp.integration.kafka.BrukernotifikasjonBeskjedSender
import no.nav.helse.fritakagp.integration.kafka.MockBrukernotifikasjonBeskjedSender
import no.nav.helse.fritakagp.integration.norg.ArbeidsfordelingRequest
import no.nav.helse.fritakagp.integration.norg.ArbeidsfordelingResponse
import no.nav.helse.fritakagp.integration.norg.Norg2Client
import no.nav.helse.fritakagp.integration.virusscan.MockVirusScanner
import no.nav.helse.fritakagp.integration.virusscan.VirusScanner
import no.nav.helse.fritakagp.service.BehandlendeEnhetService
import no.nav.helsearbeidsgiver.altinn.AltinnClient
import no.nav.helsearbeidsgiver.altinn.AltinnOrganisasjon
import org.koin.core.module.Module
import org.koin.dsl.bind
import java.time.LocalDate
import java.time.LocalDateTime

fun Module.mockExternalDependecies() {

    single {
        mockk<AltinnClient>().also {
            coEvery { it.hentRettighetOrganisasjoner(any()) } returns altinnOrgs
        }
    }

    single { MockBrukernotifikasjonBeskjedSender() } bind BrukernotifikasjonBeskjedSender::class
    single {
        object : AccessTokenProvider {
            override fun getToken(): String {
                return "fake token"
            }
        }
    } bind AccessTokenProvider::class

    single {
        object : AaregArbeidsforholdClient {
            override suspend fun hentArbeidsforhold(ident: String, callId: String): List<Arbeidsforhold> =
                listOf(
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

    single {
        object : Norg2Client(
            "",
            object : AccessTokenProvider {
                override fun getToken(): String {
                    return "token"
                }
            },
            get()
        ) {
            override suspend fun hentAlleArbeidsfordelinger(
                request: ArbeidsfordelingRequest,
                callId: String?
            ): List<ArbeidsfordelingResponse> = listOf(
                ArbeidsfordelingResponse(
                    aktiveringsdato = LocalDate.of(2020, 11, 30),
                    antallRessurser = 0,
                    enhetId = 123456789,
                    enhetNr = "1234",
                    kanalstrategi = null,
                    navn = "NAV Område",
                    nedleggelsesdato = null,
                    oppgavebehandler = false,
                    orgNivaa = "SPESEN",
                    orgNrTilKommunaltNavKontor = "",
                    organisasjonsnummer = null,
                    sosialeTjenester = "",
                    status = "Aktiv",
                    type = "KO",
                    underAvviklingDato = null,
                    underEtableringDato = LocalDate.of(2020, 11, 30),
                    versjon = 1
                )
            )
        }
    } bind Norg2Client::class

    single { BehandlendeEnhetService(get(), get()) }
}

private val altinnOrgs = setOf(
    AltinnOrganisasjon(
        name = "HØNEFOSS OG ØLEN",
        type = "Enterprise",
        organizationNumber = "910020102",
        organizationForm = "AS",
        status = "Active",
    ),
    AltinnOrganisasjon(
        name = "JØA OG SEL",
        type = "Business",
        organizationNumber = "910098896",
        organizationForm = "BEDR",
        parentOrganizationNumber = "911366940",
        status = "Active",
    ),
    AltinnOrganisasjon(
        name = "ELTRODE AS",
        type = "Business",
        organizationNumber = "917404437",
        organizationForm = "BEDR",
        parentOrganizationNumber = "917346380",
        status = "Active",
    ),
    AltinnOrganisasjon(
        name = "STADLANDET OG SINGSÅS",
        type = "Enterprise",
        organizationNumber = "911366940",
        organizationForm = "AS",
        status = "Active",
    ),
    AltinnOrganisasjon(
        name = "SKIKKELIG GJØK",
        type = "Enterprise",
        organizationNumber = "947064649",
        organizationForm = "AS",
        status = "Active",
    ),
)
