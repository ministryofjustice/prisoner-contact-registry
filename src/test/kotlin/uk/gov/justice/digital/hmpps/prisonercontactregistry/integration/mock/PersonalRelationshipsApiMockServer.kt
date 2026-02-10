package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.RestPage
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactIdsRequestDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.TestObjectMapper

class PersonalRelationshipsApiMockServer : WireMockServer(8093) {
  fun stubGetAllContacts(
    prisonerId: String,
    contacts: List<PersonalRelationshipsContactDto>,
    approvedVisitorOnly: Boolean = false,
    page: Int = 0,
    size: Int = 350,
  ) {
    val uri = "/prisoner/$prisonerId/contact"

    val response =
      aResponse()
        .withStatus(200)
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withBody(
          TestObjectMapper.mapper.writeValueAsString(
            RestPage(
              content = contacts,
              size = contacts.size,
              total = contacts.size.toLong(),
              page = page,
            ),
          ),
        )

    if (approvedVisitorOnly) {
      stubFor(
        get(urlPathEqualTo(uri))
          .withQueryParam("relationshipType", equalTo("S"))
          .withQueryParam("page", equalTo(page.toString()))
          .withQueryParam("size", equalTo(size.toString()))
          .withQueryParam("approvedVisitor", equalTo("true"))
          .willReturn(response),
      )
    } else {
      stubFor(
        get(urlPathEqualTo(uri))
          .withQueryParam("relationshipType", equalTo("S"))
          .withQueryParam("page", equalTo(page.toString()))
          .withQueryParam("size", equalTo(size.toString()))
          .willReturn(response),
      )
    }
  }

  fun stubPrisonerContactRestrictions(
    prisonerContactIds: List<Long>,
    response: PrisonerContactRestrictionsResponseDto = defaultRestrictionsResponse(prisonerContactIds),
  ) {
    val uri = "/prisoner-contact/restrictions"

    val expectedRequestJson = TestObjectMapper.mapper.writeValueAsString(PrisonerContactIdsRequestDto(prisonerContactIds))

    stubFor(
      post(urlPathEqualTo(uri))
        .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
        // ignoreArrayOrder = true, ignoreExtraElements = true
        .withRequestBody(equalToJson(expectedRequestJson, true, true))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody(TestObjectMapper.mapper.writeValueAsString(response)),
        ),
    )
  }

  /**
   * Default: one entry per prisonerContactId, no local or global restrictions
   */
  private fun defaultRestrictionsResponse(
    prisonerContactIds: List<Long>,
  ): PrisonerContactRestrictionsResponseDto = PrisonerContactRestrictionsResponseDto(
    prisonerContactRestrictions =
    prisonerContactIds.map { relationshipId ->
      PrisonerContactRestrictionsDto(
        prisonerContactId = relationshipId,
        prisonerContactRestrictions = emptyList(),
        globalContactRestrictions = emptyList(),
      )
    },
  )
}
