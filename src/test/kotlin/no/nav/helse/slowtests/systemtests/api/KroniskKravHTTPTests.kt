package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.GravidTestData
import no.nav.helse.KroniskTestData
import no.nav.helse.fritakagp.db.KroniskKravRepository
import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import no.nav.helse.fritakagp.domain.KroniskKrav
import no.nav.helse.fritakagp.web.api.resreq.ValidationProblem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.test.inject
import java.time.LocalDate

class KroniskKravHTTPTests : SystemTestBase() {
    private val kravKroniskUrl = "/api/v1/kronisk/krav"

    @Test
    internal fun `Returnerer kravet når korrekt bruker er innlogget, 404 når ikke`() = suspendableTest {
        val repo by inject<KroniskKravRepository>()
        repo.insert(KroniskTestData.kroniskKrav)

        val notFoundException = assertThrows<ClientRequestException>
        {
            httpClient.get<HttpResponse> {
                appUrl("$kravKroniskUrl/${KroniskTestData.kroniskKrav.id}")
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
            }
        }
        assertThat(notFoundException.response.status).isEqualTo(HttpStatusCode.NotFound)

        val accessGrantedForm = httpClient.get<KroniskKrav> {
            appUrl("$kravKroniskUrl/${KroniskTestData.kroniskKrav.id}")
            contentType(ContentType.Application.Json)
            loggedInAs(KroniskTestData.kroniskKrav.identitetsnummer)
        }

        assertThat(accessGrantedForm).isEqualTo(KroniskTestData.kroniskKrav)
    }

    @Test
    fun `invalid json gives 400 Bad request`() = suspendableTest {
        val responseExcepion = assertThrows<ClientRequestException> {
            httpClient.post<HttpResponse> {
                appUrl(kravKroniskUrl)
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")

                body = """
                    {
                        "identitetsnummer": "${KroniskTestData.validIdentitetsnummer}",
                        "virksomhetsnummer": "${KroniskTestData.fullValidRequest.virksomhetsnummer}",
                        "perioder": fele,
                        "bekreftet": ["IKKE GYLDIG"]
                        "dokumentasjon": ["IKKE GYLDIG"]
                        "kontrollDager": ["IKKE GYLDIG"]
                    }
                """.trimIndent()
            }
        }

        assertThat(responseExcepion.response.status).isEqualTo(HttpStatusCode.BadRequest)
        val res = extractResponseBody(responseExcepion.response)
        assertThat(res.title).contains("Feil ved prosessering av JSON-dataene som ble oppgitt")
    }

    @Test
    fun `Skal returnere Created ved feilfritt skjema uten fil`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravKroniskUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = KroniskTestData.kroniskKravRequestValid
        }

        val krav = response.receive<KroniskKrav>()
        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        assertThat(krav.identitetsnummer).isEqualTo(KroniskTestData.kroniskKravRequestValid.identitetsnummer)
    }

    @Test
    fun `Skal returnere forbidden hvis virksomheten ikke er i auth listen fra altinn`() = suspendableTest {
        val responseExcepion = assertThrows<ClientRequestException> {
            httpClient.post<HttpResponse> {
                appUrl(kravKroniskUrl)
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
                body = KroniskTestData.kroniskKravRequestValid.copy(virksomhetsnummer = "123456785")
            }
        }

        assertThat(responseExcepion.response.status).isEqualTo(HttpStatusCode.Forbidden)
    }

    @Test
    fun `Skal returnere Created når fil er vedlagt`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravKroniskUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = KroniskTestData.kroniskKravRequestMedFil
        }

        val krav = response.receive<KroniskKrav>()
        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        assertThat(krav.harVedlegg).isEqualTo(true)
    }

    @Test
    fun `Skal returnere full propertypath for periode`() = suspendableTest {
        val responseExcepion = assertThrows<ClientRequestException> {
            httpClient.post<HttpResponse> {
                appUrl(kravKroniskUrl)
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
                body = KroniskTestData.kroniskKravRequestInValid.copy(
                    perioder = listOf(
                        Arbeidsgiverperiode(
                            LocalDate.of(2020, 2, 1),
                            LocalDate.of(2020, 1, 31),
                            29,
                            månedsinntekt = 34000000.0
                        ),
                        Arbeidsgiverperiode(
                            LocalDate.of(2020, 2, 3),
                            LocalDate.of(2020, 1, 31),
                            23,
                            månedsinntekt = -30.0
                        ),
                        Arbeidsgiverperiode(
                            LocalDate.of(2020, 1, 5),
                            LocalDate.of(2020, 1, 14),
                            12,
                            månedsinntekt = 2590.8,
                        )
                    )
                )
            }
        }
        val possiblePropertyPaths = listOf(
            "perioder[0].fom",
            "perioder[0].månedsinntekt",
            "perioder[0].antallDagerMedRefusjon",
            "perioder[1].fom",
            "perioder[1].antallDagerMedRefusjon",
            "perioder[1].månedsinntekt",
            "perioder[2].antallDagerMedRefusjon",
        )
        val res = responseExcepion.response.call.receive<ValidationProblem>()
        assertThat(res.violations.size).isEqualTo(7)
        res.violations.forEach {
            assertThat(it.propertyPath).isIn(possiblePropertyPaths)
        }
    }

    @Test
    fun `Virksomhetsrute returnerer liste over krav når korrekt bruker er innlogget`() = suspendableTest {
        val repo by inject<KroniskKravRepository>()
        repo.insert(KroniskTestData.kroniskKrav)

        val kroniskKravListe = httpClient.get<List<KroniskKrav>> {
            appUrl("$kravKroniskUrl/virksomhet/${GravidTestData.validOrgNr}")
            contentType(ContentType.Application.Json)
            loggedInAs(KroniskTestData.kroniskKrav.identitetsnummer)
        }

        assertThat(kroniskKravListe.find { it.id == KroniskTestData.kroniskKrav.id }).isEqualTo(KroniskTestData.kroniskKrav)
    }

    @Test
    fun `Virksomhetsrute skal returnere forbidden hvis virksomheten ikke er i auth listen fra altinn`() = suspendableTest {
        val responseException = assertThrows<ClientRequestException> {
            httpClient.get<HttpResponse> {
                appUrl("$kravKroniskUrl/virksomhet/123456785")
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
            }
        }
        assertThat(responseException.response.status).isEqualTo(HttpStatusCode.Forbidden)
    }
}
