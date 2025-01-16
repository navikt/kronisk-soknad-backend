package no.nav.helse.fritakagp.koin

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.OppgaveResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.OpprettOppgaveResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.Prioritet
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave2.Status
import no.nav.helse.fritakagp.auth.AuthClient
import no.nav.helse.fritakagp.auth.TokenResponse
import no.nav.helse.fritakagp.integration.brreg.BrregClient
import no.nav.helse.fritakagp.integration.brreg.MockBrregClient
import no.nav.helse.fritakagp.integration.gcp.BucketStorage
import no.nav.helse.fritakagp.integration.gcp.MockBucketStorage
import no.nav.helse.fritakagp.integration.kafka.BrukernotifikasjonSender
import no.nav.helse.fritakagp.integration.kafka.MockBrukernotifikasjonBeskjedSender
import no.nav.helse.fritakagp.integration.virusscan.MockVirusScanner
import no.nav.helse.fritakagp.integration.virusscan.VirusScanner
import no.nav.helse.fritakagp.processing.arbeidsgivernotifikasjon.ArbeidsgiverOppdaterNotifikasjonProcessor
import no.nav.helsearbeidsgiver.aareg.AaregClient
import no.nav.helsearbeidsgiver.aareg.Ansettelsesperiode
import no.nav.helsearbeidsgiver.aareg.Arbeidsforhold
import no.nav.helsearbeidsgiver.aareg.Arbeidsgiver
import no.nav.helsearbeidsgiver.aareg.Opplysningspliktig
import no.nav.helsearbeidsgiver.aareg.Periode
import no.nav.helsearbeidsgiver.altinn.Altinn3OBOClient
import no.nav.helsearbeidsgiver.altinn.AltinnClient
import no.nav.helsearbeidsgiver.altinn.AltinnOrganisasjon
import no.nav.helsearbeidsgiver.altinn.AltinnTilgangRespons
import no.nav.helsearbeidsgiver.dokarkiv.DokArkivClient
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.helsearbeidsgiver.pdl.domene.FullPerson
import no.nav.helsearbeidsgiver.pdl.domene.PersonNavn
import no.nav.helsearbeidsgiver.tokenprovider.AccessTokenProvider
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import java.time.LocalDate
import java.time.LocalDateTime

fun Module.mockExternalDependecies() {
    single {
        mockk<AltinnClient> {
            val json = Json { ignoreUnknownKeys = true }
            val jsonFile = "altinn-mock/organisasjoner-med-rettighet.json".loadFromResources()
            val altinnOrganisasjons = json.decodeFromString<List<AltinnOrganisasjon>>(jsonFile).toSet()

            coEvery { hentRettighetOrganisasjoner(any()) } returns altinnOrganisasjons
            coEvery { harRettighetForOrganisasjon(any(), any()) } answers {
                val organisasjonsNr = secondArg<String>()
                altinnOrganisasjons.any {
                    it.orgnr == organisasjonsNr && it.orgnrHovedenhet != null
                }
            }
        }
    }
    single {
        mockk<AuthClient> {
            coEvery { exchange(any(), any(), any()) } returns
                TokenResponse.Success(
                    "",
                    3599
                )
        }
    }

    single {
        mockk<Altinn3OBOClient> {
            val json = Json { ignoreUnknownKeys = true }
            val jsonFile = "altinn-mock/rettighetene-til-tanja-minge.json".loadFromResources()
            val tilgangRespons = json.decodeFromString<AltinnTilgangRespons>(jsonFile)

            coEvery { hentHierarkiMedTilganger(any(), any()) } returns tilgangRespons
            coEvery { harTilgangTilOrganisasjon(any(), any(), any()) } answers {
                val organisasjonsNr = secondArg<String>()
                tilgangRespons.tilgangTilOrgNr["4936:1"]?.contains(organisasjonsNr) ?: false
            }
        }
    }

    single { MockBrukernotifikasjonBeskjedSender() } bind BrukernotifikasjonSender::class
    single(named("TOKENPROVIDER")) {
        object : AccessTokenProvider {
            override fun getToken(): String {
                return "fake token"
            }
        }
    } bind AccessTokenProvider::class

    single {
        mockk<AaregClient> {
            coEvery { hentArbeidsforhold(any(), any()) } returns listOf(
                Arbeidsforhold(
                    Arbeidsgiver("test", "810007842"),
                    Opplysningspliktig("Juice", "810007702"),
                    emptyList(),
                    Ansettelsesperiode(
                        Periode(LocalDate.MIN, null)
                    ),
                    LocalDate.MIN.atStartOfDay()
                ),
                Arbeidsforhold(
                    Arbeidsgiver("test", "910098896"),
                    Opplysningspliktig("Juice", "910098896"),
                    emptyList(),
                    Ansettelsesperiode(
                        Periode(
                            LocalDate.MIN,
                            null
                        )
                    ),
                    LocalDate.MIN.atStartOfDay()
                ),
                Arbeidsforhold(
                    Arbeidsgiver("test", "917404437"),
                    Opplysningspliktig("Juice", "910098896"),
                    emptyList(),
                    Ansettelsesperiode(
                        Periode(
                            LocalDate.MIN,
                            null
                        )
                    ),
                    LocalDate.MIN.atStartOfDay()
                )
            )
        }
    }

    single {
        val tokenProvider: AccessTokenProvider = get(qualifier = named("TOKENPROVIDER"))
        DokArkivClient("url", 3, tokenProvider::getToken)
    } bind DokArkivClient::class

    single {
        mockk<PdlClient> {
            coEvery { personNavn(any()) } returns PersonNavn("Ola", "M", "Avsender")
            coEvery { fullPerson(any()) } returns FullPerson(
                navn = PersonNavn(fornavn = "Per", mellomnavn = "", etternavn = "Ulv"),
                foedselsdato = LocalDate.of(1900, 1, 1),
                ident = "aktør-id",
                diskresjonskode = "SPSF",
                geografiskTilknytning = "SWE"
            )
        }
    }

    single {
        object : OppgaveKlient {
            override suspend fun hentOppgave(oppgaveId: Int, callId: String): OppgaveResponse {
                return OppgaveResponse(oppgaveId, 1, oppgavetype = "JFR", aktivDato = LocalDateTime.now().minusDays(3).toLocalDate(), prioritet = Prioritet.NORM.toString())
            }

            override suspend fun opprettOppgave(
                opprettOppgaveRequest: OpprettOppgaveRequest,
                callId: String
            ): OpprettOppgaveResponse = OpprettOppgaveResponse(
                1234,
                "0100",
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

    single { mockk<ArbeidsgiverOppdaterNotifikasjonProcessor>(relaxed = true) }
}
fun String.loadFromResources(): String {
    return ClassLoader.getSystemResource(this).readText()
}
