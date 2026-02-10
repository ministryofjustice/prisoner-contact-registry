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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PageMetadata
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PagedResponse
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactIdsRequestDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.TestObjectMapper

class PersonalRelationshipsApiMockServer : WireMockServer(8093) {
  fun stubGetAllContacts(
    prisonerId: String,
    contacts: List<PersonalRelationshipsContactDto>? = null,
    approvedVisitorOnly: Boolean = false,
    page: Int = 0,
    size: Int = 350,
    httpStatus: HttpStatus = HttpStatus.OK,
  ) {
    val uri = "/prisoner/$prisonerId/contact"

    val response = if (contacts == null) {
      aResponse().withStatus(httpStatus.value())
    } else {
      val totalElements = contacts.size.toLong()
      val totalPages = if (size <= 0) 0 else kotlin.math.ceil(totalElements.toDouble() / size.toDouble()).toInt()

      aResponse()
        .withStatus(httpStatus.value())
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withBody(
          TestObjectMapper.mapper.writeValueAsString(
            PagedResponse(
              content = contacts,
              page = PageMetadata(
                size = size,
                number = page,
                totalElements = totalElements,
                totalPages = totalPages,
              ),
            ),
          ),
        )
    }

    val mapping = get(urlPathEqualTo(uri))
      .withQueryParam("relationshipType", equalTo("S"))
      .withQueryParam("page", equalTo(page.toString()))
      .withQueryParam("size", equalTo(size.toString()))
      .apply {
        if (approvedVisitorOnly) withQueryParam("approvedVisitor", equalTo("true"))
      }
      .willReturn(response)

    stubFor(mapping)
  }

  fun stubPrisonerContactRestrictions(
    prisonerContactIds: List<Long>,
    response: PrisonerContactRestrictionsResponseDto? = defaultRestrictionsResponse(prisonerContactIds),
    httpStatus: HttpStatus = HttpStatus.OK,
  ) {
    val uri = "/prisoner-contact/restrictions"

    val expectedRequestJson =
      TestObjectMapper.mapper.writeValueAsString(
        PrisonerContactIdsRequestDto(prisonerContactIds),
      )

    val wiremockResponse =
      if (response == null) {
        aResponse()
          .withStatus(httpStatus.value())
      } else {
        aResponse()
          .withStatus(httpStatus.value())
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .withBody(TestObjectMapper.mapper.writeValueAsString(response))
      }

    stubFor(
      post(urlPathEqualTo(uri))
        .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
        // ignoreArrayOrder = true, ignoreExtraElements = true
        .withRequestBody(equalToJson(expectedRequestJson, true, true))
        .willReturn(wiremockResponse),
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
