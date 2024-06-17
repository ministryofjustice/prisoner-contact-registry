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
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactsDto

class PrisonApiMockServer : WireMockServer(8092) {
  fun stubGetOffenderContacts(offenderNo: String, contacts: ContactsDto) {
    stubFor(
      get("/api/offenders/$offenderNo/contacts")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody(getJsonString(contacts)),
        ),
    )
  }

  fun stubGetApprovedOffenderContacts(
    offenderNo: String,
    contacts: ContactsDto? = null,
    httpStatus: HttpStatus = NOT_FOUND,
  ) {
    stubFor(
      get("/api/offenders/$offenderNo/contacts?approvedVisitorsOnly=true")
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

  fun stubGetOffenderNotFound(offenderNo: String) {
    stubFor(
      get("/api/offenders/$offenderNo/contacts")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(HttpStatus.NOT_FOUND.value()),
        ),
    )
  }

  fun stubGetOffenderBadRequest(offenderNo: String) {
    stubFor(
      get("/api/offenders/$offenderNo/contacts")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(HttpStatus.BAD_REQUEST.value()),
        ),
    )
  }

  fun stubGetPersonAddressesEmpty(personId: Long) {
    stubFor(
      get("/api/persons/$personId/addresses")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody(
              """
                []
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubGetPersonAddressesFullAddress(personId: Long) {
    stubFor(
      get("/api/persons/$personId/addresses")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody(
              """
                [
                  {
                      "addressId": 2593598,
                      "addressType": "BUS",
                      "flat": "3B",
                      "premise": "Liverpool Prison",
                      "street": "Slinn Street",
                      "locality": "Brincliffe",
                      "town": "Birmingham",
                      "postalCode": "D7 5CC",
                      "county": "West Midlands",
                      "country": "England",
                      "comment": "Comment Here",
                      "primary": true,
                      "noFixedAddress": false,
                      "startDate": "2012-05-01",
                      "endDate": "2016-05-01",
                      "phones": [
                        {
                            "phoneId": 620163,
                            "number": "504 555 24302",
                            "type": "BUS",
                            "ext": "123"
                        }
                      ],
                      "addressUsages": [
                        {
                          "addressId": "23422313",
                          "addressUsage": "HDC",
                          "addressUsageDescription": "HDC Address",
                          "activeFlag": true
                        }
                      ]
                  }
              ]
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubGetPersonAddressesMinimumAddress(personId: Long) {
    stubFor(
      get("/api/persons/$personId/addresses")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody(
              """
                [
                  {
                      "primary": true,
                      "noFixedAddress": false,
                      "phones": [
                        {
                            "phoneId": 620163,
                            "number": "504 555 24302",
                            "type": "BUS"
                        }
                      ],
                      "addressUsages": [
                        {
                          "addressId": "23422313"
                        }
                      ]
                  }
              ]
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubGetPersonNotFound(personId: Long) {
    stubFor(
      get("/api/persons/$personId/addresses")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(404),
        ),
    )
  }

  private fun getJsonString(obj: Any): String {
    return ObjectMapper()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .registerModule(JavaTimeModule())
      .writer()
      .withDefaultPrettyPrinter()
      .writeValueAsString(obj)
  }
}
