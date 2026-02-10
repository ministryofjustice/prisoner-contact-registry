package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsContactDto

class PersonalRelationshipsApiMockServer : WireMockServer(8093) {
  fun stubGetPrisonerSocialContacts(
    prisonerId: String,
    contacts: List<PersonalRelationshipsContactDto>? = null,
    httpStatus: HttpStatus = NOT_FOUND,
  ) {
    stubFor(
      get("/prisoner/$prisonerId/contact?relationshipType=S&page=0&size=350")
        .willReturn(
          if (contacts != null) {
            aResponse()
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withStatus(200)
              .withBody(getJsonString(contacts))
          } else {
            aResponse()
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withStatus(httpStatus.value())
          },
        ),
    )
  }

  fun stubGetApprovedPrisonerContacts(
    prisonerId: String,
    contacts: List<PersonalRelationshipsContactDto>? = null,
    httpStatus: HttpStatus = NOT_FOUND,
  ) {
    stubFor(
      get("/prisoner/$prisonerId/contact?relationshipType=S&page=0&size=350&approvedVisitor=true")
        .willReturn(
          if (contacts != null) {
            aResponse()
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withStatus(200)
              .withBody(getJsonString(contacts))
          } else {
            aResponse()
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withStatus(httpStatus.value())
          },
        ),
    )
  }

  private fun getJsonString(obj: Any): String = ObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())
    .writer()
    .withDefaultPrettyPrinter()
    .writeValueAsString(obj)
}
