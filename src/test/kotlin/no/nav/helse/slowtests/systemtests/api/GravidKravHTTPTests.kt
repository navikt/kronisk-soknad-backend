package no.nav.helse.slowtests.systemtests.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.db.GravidKravRepository
import no.nav.helse.fritakagp.domain.GravidKrav
import no.nav.helse.fritakagp.integration.brreg.BerregClientImp
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.koin.test.inject
import kotlin.test.assertFailsWith

class GravidKravHTTPTests : SystemTestBase() {
    private val kravGravidUrl = "/api/v1/gravid/krav"
    private val enhetsUrl ="data.brreg.no/enhetsregisteret/api/enheter"
    @Test
    internal fun `Returnerer kravet når korrekt bruker er innlogget, 404 når ikke`() = suspendableTest {
        val repo by inject<GravidKravRepository>()

        repo.insert(GravidTestData.gravidKrav)

        val accessDenied = httpClient.get<HttpResponse> {
            appUrl("$kravGravidUrl/${GravidTestData.gravidKrav.id}")
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
        }

        Assertions.assertThat(accessDenied.status).isEqualTo(HttpStatusCode.NotFound)

        val accessGrantedForm = httpClient.get<GravidKrav> {
            appUrl("$kravGravidUrl/${GravidTestData.gravidKrav.id}")
            contentType(ContentType.Application.Json)
            loggedInAs(GravidTestData.gravidKrav.identitetsnummer)
        }

        Assertions.assertThat(accessGrantedForm).isEqualTo(GravidTestData.gravidKrav)
    }


    @Test
    fun `invalid json gives 400 Bad request`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
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

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
    }

    @Test
    fun `Skal returnere Created ved feilfritt skjema uten fil`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.gravidKravRequestValid
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.Created)
    }

    @Test
    fun `Skal returnere forbidden hvis virksomheten ikke er i auth listen fra altinn`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.gravidKravRequestValid.copy(virksomhetsnummer = "123456785")
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
    }

    @Test
    fun `Skal returnere Created og lagre flagg når fil er vedlagt`() = suspendableTest {
        val response = httpClient.post<HttpResponse> {
            appUrl(kravGravidUrl)
            contentType(ContentType.Application.Json)
            loggedInAs("123456789")
            body = GravidTestData.gravidKravRequestMedFil
        }

        Assertions.assertThat(response.status).isEqualTo(HttpStatusCode.Created)
    }

    @Test
    fun `Skal returnere ARBEIDS- OG VELFERDSETATEN`() = suspendableTest {
        val om = ObjectMapper()
        val client = BerregClientImp(httpClient, om, enhetsUrl)
        val navn = client.getVirksomhetsNavn("889640782")
        Assertions.assertThat(navn).isEqualTo("ARBEIDS- OG VELFERDSETATEN")
    }

    @Test
    fun `Skal returnere 404`() = suspendableTest {
        val om = ObjectMapper()
        val fakeOrgNr = "123456789"
        val errmsg = "Client request(https://${enhetsUrl}/$fakeOrgNr) invalid: 404"
        val client = BerregClientImp(httpClient, om, enhetsUrl)
        val exception = assertFailsWith<ClientRequestException> { client.getVirksomhetsNavn(fakeOrgNr) }
        Assertions.assertThat(exception.message).contains(errmsg)
    }
}