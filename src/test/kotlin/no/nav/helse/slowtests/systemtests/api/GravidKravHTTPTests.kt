package no.nav.helse.slowtests.systemtests.api

import io.ktor.client.call.receive
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.domain.Arbeidsgiverperiode
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.web.api.resreq.ValidationProblem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.test.inject
import java.time.LocalDate

class GravidKravHTTPTests : SystemTestBase() {
    private val kravGravidUrl = "/api/v1/gravid/krav"

    @Test
    @Disabled
    internal fun `Returnerer kravet når korrekt bruker er innlogget, 403 når ikke`() = suspendableTest {
        val repo by inject<GravidKravRepository>()

        repo.insert(GravidTestData.gravidKrav)
        val exception = assertThrows<ClientRequestException>
        {
            httpClient.get<HttpResponse> {
                appUrl("$kravGravidUrl/${GravidTestData.gravidKrav.id}")
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
            }
        }

        assertThat(exception.response.status).isEqualTo(HttpStatusCode.Forbidden)

        val accessGrantedForm = httpClient.get<GravidKrav> {
            appUrl("$kravGravidUrl/${GravidTestData.gravidKrav.id}")
            contentType(ContentType.Application.Json)
            loggedInAs(GravidTestData.gravidKrav.identitetsnummer)
        }

        assertThat(accessGrantedForm).isEqualTo(GravidTestData.gravidKrav)
    }

    @Test
    fun `invalid json gives 400 Bad request`() = suspendableTest {
        val responseExcepion = assertThrows<IllegalStateException> {
            httpClient.post<HttpResponse> {
                appUrl(kravGravidUrl)
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")

                body = """
                {
                    "fnr": "${GravidTestData.validIdentitetsnummer}",
                    "orgnr": "${GravidTestData.fullValidSoeknadRequest.virksomhetsnummer}",
                    "tilrettelegge": true,
                    "tiltak": ["IKKE GYLDIG"]
                }
                """.trimIndent()
            }
        }

//        assertThat(responseExcepion.response.status).isEqualTo(HttpStatusCode.BadRequest)
//        val res = extractResponseBody(responseExcepion.response)
//        assertThat(res.title).contains("Feil ved prosessering av JSON-dataene som ble oppgitt")
    }

    @Test
    fun `Skal returnere Created ved feilfritt skjema uten fil`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.gravidKravRequestValid
        }

        val krav = response.receive<GravidKrav>()
        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        assertThat(krav.identitetsnummer).isEqualTo(GravidTestData.gravidKravRequestValid.identitetsnummer)
    }

    @Test
    fun `Skal returnere Created ved periode på en dag`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.gravidKravRequestValidPeriode1Dag
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        val krav = response.receive<GravidKrav>()
        assertThat(krav.identitetsnummer).isEqualTo(GravidTestData.gravidKravRequestValidPeriode1Dag.identitetsnummer)
    }

    @Test
    fun `Skal returnere forbidden hvis virksomheten ikke er i auth listen fra altinn`() = suspendableTest {
        val responseExcepion = assertThrows<ClientRequestException> {
            httpClient.post<HttpResponse> {
                appUrl(kravGravidUrl)
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
                body = GravidTestData.gravidKravRequestValid.copy(virksomhetsnummer = "123456785")
            }
        }
        assertThat(responseExcepion.response.status).isEqualTo(HttpStatusCode.Forbidden)
    }

    @Test
    fun `Skal returnere Created og lagre flagg når fil er vedlagt`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.gravidKravRequestMedFil
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
    }

    @Test
    fun `Skal returnere en valideringfeil`() = suspendableTest {
        val responseExcepion = assertThrows<ClientRequestException> {
            httpClient.post<HttpResponse> {
                appUrl(kravGravidUrl)
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
                body = GravidTestData.gravidKravRequestInValid.copy(
                    perioder = listOf(
                        Arbeidsgiverperiode(
                            LocalDate.of(2020, 1, 15),
                            LocalDate.of(2020, 1, 10),
                            2,
                            månedsinntekt = 2590.8
                        ),
                        Arbeidsgiverperiode(
                            LocalDate.of(2020, 1, 5),
                            LocalDate.of(2020, 1, 4),
                            2,
                            månedsinntekt = 2590.8,
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

        assertThat(responseExcepion.response.status).isEqualTo(HttpStatusCode.UnprocessableEntity)
        val res = responseExcepion.response.call.receive<ValidationProblem>()
        assertThat(res.violations.size).isEqualTo(5)
    }

    @Test
    fun `Skal returnere full propertypath for periode`() = suspendableTest {
        val responseExcepion = assertThrows<ClientRequestException> {
            httpClient.post<HttpResponse> {
                appUrl(kravGravidUrl)
                contentType(ContentType.Application.Json)
                loggedInAs("123456789")
                body = GravidTestData.gravidKravRequestInValid.copy(
                    perioder = listOf(
                        Arbeidsgiverperiode(
                            LocalDate.of(2020, 1, 15),
                            LocalDate.of(2020, 1, 10),
                            2,
                            månedsinntekt = 2590.8
                        ),
                        Arbeidsgiverperiode(
                            LocalDate.of(2020, 1, 5),
                            LocalDate.of(2020, 1, 4),
                            2,
                            månedsinntekt = 2590.8,
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
        val possiblePropertyPaths = setOf(
            "perioder[0].fom",
            "perioder[0].antallDagerMedRefusjon",
            "perioder[1].fom",
            "perioder[1].antallDagerMedRefusjon",
            "perioder[2].antallDagerMedRefusjon",
        )
        val res = responseExcepion.response.call.receive<ValidationProblem>()
        assertThat(res.violations.size).isEqualTo(5)
        res.violations.forEach {
            assertThat(it.propertyPath).isIn(possiblePropertyPaths)
        }
    }
}
