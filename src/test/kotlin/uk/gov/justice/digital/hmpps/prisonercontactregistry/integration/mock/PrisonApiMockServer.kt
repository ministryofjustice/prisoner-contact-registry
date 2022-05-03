package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.MediaType

class PrisonApiMockServer : WireMockServer(8092) {

  fun stubGetOffenderContactsEmpty(offenderNo: String) {
    stubFor(
      get("/api/offenders/$offenderNo/contacts")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody(
              """
                {
                "offenderContacts": []
                }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetOffenderContactFullContact(offenderNo: String) {
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
                    "lastName": "Ireron",
                    "middleName": "Danger",
                    "firstName": "Ehicey",
                    "dateOfBirth": "1912-09-13",
                    "contactType": "O",
                    "contactTypeDescription": "Official",
                    "relationshipCode": "PROB",
                    "relationshipDescription": "Probation Officer",
                    "commentText": "Comment Here",
                    "emergencyContact": false,
                    "nextOfKin": false,
                    "personId": 2187521,
                    "approvedVisitor": false,
                    "bookingId": 1111405,
                    "emails": [
                        {
                            "email": "yucSZp.RwyKbcLgEL@VED.sOt.upz.Lc"
                        }
                    ],
                    "restrictions": [
                      {
                          "restrictionId": 22022,
                          "comment": "Comment Here",
                          "restrictionType": "BAN",
                          "restrictionTypeDescription": "Banned",
                          "startDate": "2012-09-13",
                          "expiryDate": "2014-09-13",
                          "globalRestriction": false
                      }
                    ]
                  }
                ]
                }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetOffenderContactMinimumContact(offenderNo: String) {
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
                    "lastName": "Ireron",
                    "firstName": "Ehicey",
                    "contactType": "O",
                    "relationshipCode": "PROB",
                    "emergencyContact": false,
                    "nextOfKin": false,
                    "approvedVisitor": false,
                    "bookingId": 1111405,
                    "restrictions": [
                      {
                          "restrictionId": 22022,
                          "restrictionType": "BAN",
                          "restrictionTypeDescription": "Banned",
                          "startDate": "2012-09-13",
                          "globalRestriction": false
                      }
                    ]
                  }
                ]
                }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetOffenderNotFound(offenderNo: String) {
    stubFor(
      get("/api/offenders/$offenderNo/contacts")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(404)
        )
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
              """.trimIndent()
            )
        )
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
              """.trimIndent()
            )
        )
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
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetPersonNotFound(personId: Long) {
    stubFor(
      get("/api/persons/$personId/addresses")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(404)
        )
    )
  }
}
