package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.HttpStatus
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
    contacts: ContactsDto?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
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

  fun stubGetOffenderContactsForOrderingByNames(offenderNo: String) {
    stubFor(
      get("/api/offenders/$offenderNo/contacts")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody(
              """
                {
                "offenderContacts": [
                  {
                    "lastName": "Gwyn",
                    "firstName": "Wyn",
                    "contactType": "O",
                    "relationshipCode": "PROB",
                    "emergencyContact": false,
                    "nextOfKin": false,
                    "approvedVisitor": false,
                    "bookingId": 1111405
                  },
                  {
                    "lastName": "Gwyn",
                    "firstName": "Aeron",
                    "contactType": "O",
                    "relationshipCode": "PROB",
                    "emergencyContact": false,
                    "nextOfKin": false,
                    "approvedVisitor": false,
                    "bookingId": 1111405
                  },
                  {
                    "lastName": "Gwyn",
                    "firstName": "Cynog",
                    "contactType": "O",
                    "relationshipCode": "PROB",
                    "emergencyContact": false,
                    "nextOfKin": false,
                    "approvedVisitor": false,
                    "bookingId": 1111405
                  },
                  {
                    "lastName": "Llywelyn",
                    "firstName": "Gruffydd",
                    "contactType": "O",
                    "relationshipCode": "PROB",
                    "emergencyContact": false,
                    "nextOfKin": false,
                    "approvedVisitor": false,
                    "bookingId": 1111405
                  },                  
                  {
                    "lastName": "Aled",
                    "firstName": "Cynog",
                    "contactType": "O",
                    "relationshipCode": "PROB",
                    "emergencyContact": false,
                    "nextOfKin": false,
                    "approvedVisitor": false,
                    "bookingId": 1111405
                  },
                  {
                    "lastName": "Aled",
                    "firstName": "Wyn",
                    "contactType": "O",
                    "relationshipCode": "PROB",
                    "emergencyContact": false,
                    "nextOfKin": false,
                    "approvedVisitor": false,
                    "bookingId": 1111405
                  },
                  {
                    "lastName": "Aled",
                    "firstName": "Aeron",
                    "contactType": "O",
                    "relationshipCode": "PROB",
                    "emergencyContact": false,
                    "nextOfKin": false,
                    "approvedVisitor": false,
                    "bookingId": 1111405
                  }
                ]
                }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubGetOffenderNotFound(offenderNo: String) {
    stubFor(
      get("/api/offenders/$offenderNo/contacts")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(404),
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
