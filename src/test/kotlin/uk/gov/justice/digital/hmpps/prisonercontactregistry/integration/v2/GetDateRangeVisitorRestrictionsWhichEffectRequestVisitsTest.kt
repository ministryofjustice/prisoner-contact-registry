package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.v2

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.DateRangeDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.visit.scheduler.RequestVisitVisitorRestrictionsBodyDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.TestObjectMapper
import java.time.LocalDate
import kotlin.Array
import kotlin.jvm.java

@Suppress("ClassName")
class GetDateRangeVisitorRestrictionsWhichEffectRequestVisitsTest : IntegrationTestBase() {
  @Nested
  inner class authentication {
    @Test
    fun `Get date ranges for visitors restrictions which effect request visits - requires authentication`() {
      // Given
      val requestDto = RequestVisitVisitorRestrictionsBodyDto(
        prisonerId = "A1234AA",
        visitorIds = listOf("2187525"),
        supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
        currentDateRange = DateRangeDto(
          fromDate = LocalDate.now().plusDays(2),
          toDate = LocalDate.now().plusDays(28),
        ),
      )

      // When
      val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf()))

      // Then
      result.expectStatus().isForbidden
    }

    @Test
    fun `Get date ranges for visitors restrictions which effect request visits - requires correct role PRISONER_CONTACT_REGISTRY`() {
      // Given
      val prisonerId = "A1234AA"
      val visitorIds = listOf("2187525")

      val requestDto = RequestVisitVisitorRestrictionsBodyDto(
        prisonerId = prisonerId,
        visitorIds = visitorIds,
        supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
        currentDateRange = DateRangeDto(
          fromDate = LocalDate.now().plusDays(2),
          toDate = LocalDate.now().plusDays(28),
        ),
      )

      val prisonerContactIds = listOf(999001L)
      val prContacts = createPersonalRelationshipsContactDtoList(
        contactIds = listOf(2187525L),
        prisonerContactIds = prisonerContactIds,
        isApproved = true,
      )

      personalRelationshipsApiMockServer.stubGetAllContacts(
        prisonerId = prisonerId,
        contacts = prContacts,
        approvedVisitorOnly = true,
      )

      personalRelationshipsApiMockServer.stubPrisonerContactRestrictions(
        prisonerContactIds = prisonerContactIds,
      )

      // When
      val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

      // Then
      result.expectStatus().isOk
    }
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - internal server error`() {
    // Given
    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = "AA12345",
      visitorIds = listOf(),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().is5xxServerError
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - visitorId not found within list of prisoner contacts`() {
    // Given
    val prisonerId = "A1234AA"

    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = prisonerId,
      visitorIds = listOf("2187524", "2187525"),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    val prisonerContactIds = listOf(999001L)
    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = listOf(2187524L),
      prisonerContactIds = prisonerContactIds,
      isApproved = true,
    )

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = prContacts,
      approvedVisitorOnly = true,
    )

    personalRelationshipsApiMockServer.stubPrisonerContactRestrictions(
      prisonerContactIds = prisonerContactIds,
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().isNotFound
    result.expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("One of the visitors provided could not found")
      .jsonPath("$.developerMessage")
      .isEqualTo("Not all visitors provided (${requestDto.visitorIds}) are contacts for prisoner ${requestDto.prisonerId}")
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - No restrictions found for visitors`() {
    // Given
    val prisonerId = "A1234AA"
    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = prisonerId,
      visitorIds = listOf("2187524", "2187525"),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    val prisonerContactIds = listOf(999001L, 999002L)
    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = listOf(2187524L, 2187525L),
      prisonerContactIds = prisonerContactIds,
      isApproved = true,
    )

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = prContacts,
      approvedVisitorOnly = true,
    )

    personalRelationshipsApiMockServer.stubPrisonerContactRestrictions(
      prisonerContactIds = prisonerContactIds,
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().isOk
    val foundDateRanges = getResults(result)
    Assertions.assertThat(foundDateRanges).isEmpty()
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - Restriction with no expiry date found for visitors, later start date`() {
    // Given
    val prisonerId = "A1234AA"

    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = prisonerId,
      visitorIds = listOf("2187524", "2187525"),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    val prisonerContactIds = listOf(999001L, 999002L)
    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = listOf(2187524L, 2187525L),
      prisonerContactIds = prisonerContactIds,
      isApproved = true,
    )

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = prContacts,
      approvedVisitorOnly = true,
    )

    val restrictionResponse = PrisonerContactRestrictionsResponseDto(
      prisonerContactRestrictions = listOf(
        PrisonerContactRestrictionsDto(
          prisonerContactId = prisonerContactIds[0],
          prisonerContactRestrictions = listOf(
            createLocalRestriction(
              prisonerContactRestrictionId = 123L,
              prisonerContactId = prisonerContactIds[0],
              contactId = 2187524L,
              prisonerNumber = prisonerId,
              restrictionType = "CHILD",
              restrictionTypeDescription = "Banned",
              startDate = LocalDate.now().plusDays(5),
              expiryDate = null,
            ),
          ),
          globalContactRestrictions = emptyList(),
        ),
        PrisonerContactRestrictionsDto(
          prisonerContactId = prisonerContactIds[1],
          prisonerContactRestrictions = listOf(
            createLocalRestriction(
              prisonerContactRestrictionId = 124L,
              prisonerContactId = prisonerContactIds[1],
              contactId = 2187525L,
              prisonerNumber = prisonerId,
              restrictionType = "OTHER",
              restrictionTypeDescription = "Banned",
              startDate = LocalDate.now(),
              expiryDate = LocalDate.now().plusDays(1),
            ),
          ),
          globalContactRestrictions = emptyList(),
        ),
      ),
    )

    personalRelationshipsApiMockServer.stubPrisonerContactRestrictions(
      prisonerContactIds = prisonerContactIds,
      response = restrictionResponse,
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().isOk
    val foundDateRanges = getResults(result)
    val expected = listOf(DateRangeDto(fromDate = LocalDate.now().plusDays(5), toDate = LocalDate.now().plusDays(28)))
    Assertions.assertThat(foundDateRanges).isEqualTo(expected)
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - Restriction with no expiry date found for visitors, earlier start date`() {
    // Given
    val prisonerId = "A1234AA"

    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = prisonerId,
      visitorIds = listOf("2187524", "2187525"),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    val prisonerContactIds = listOf(999001L, 999002L)
    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = listOf(2187524L, 2187525L),
      prisonerContactIds = prisonerContactIds,
      isApproved = true,
    )

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = prContacts,
      approvedVisitorOnly = true,
    )

    val restrictionResponse = PrisonerContactRestrictionsResponseDto(
      prisonerContactRestrictions = listOf(
        PrisonerContactRestrictionsDto(
          prisonerContactId = prisonerContactIds[0],
          prisonerContactRestrictions = listOf(
            createLocalRestriction(
              prisonerContactRestrictionId = 123L,
              prisonerContactId = prisonerContactIds[0],
              contactId = 2187524L,
              prisonerNumber = prisonerId,
              restrictionType = "CHILD",
              restrictionTypeDescription = "Banned",
              startDate = LocalDate.now().minusDays(1),
              expiryDate = null,
            ),
          ),
          globalContactRestrictions = emptyList(),
        ),
        PrisonerContactRestrictionsDto(
          prisonerContactId = prisonerContactIds[1],
          prisonerContactRestrictions = listOf(
            createLocalRestriction(
              prisonerContactRestrictionId = 124L,
              prisonerContactId = prisonerContactIds[1],
              contactId = 2187525L,
              prisonerNumber = prisonerId,
              restrictionType = "OTHER",
              restrictionTypeDescription = "Banned",
              startDate = LocalDate.now(),
              expiryDate = LocalDate.now().plusDays(1),
            ),
          ),
          globalContactRestrictions = emptyList(),
        ),
      ),
    )

    personalRelationshipsApiMockServer.stubPrisonerContactRestrictions(
      prisonerContactIds = prisonerContactIds,
      response = restrictionResponse,
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().isOk
    val foundDateRanges = getResults(result)
    val expected = listOf(DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)))

    Assertions.assertThat(foundDateRanges).isEqualTo(expected)
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - Restriction found and date ranges returned`() {
    // Given
    val prisonerId = "A1234AA"

    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = prisonerId,
      visitorIds = listOf("2187524", "2187525"),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    val prisonerContactIds = listOf(999001L, 999002L)
    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = listOf(2187524L, 2187525L),
      prisonerContactIds = prisonerContactIds,
      isApproved = true,
    )

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = prContacts,
      approvedVisitorOnly = true,
    )

    val restrictionResponse = PrisonerContactRestrictionsResponseDto(
      prisonerContactRestrictions = listOf(
        PrisonerContactRestrictionsDto(
          prisonerContactId = prisonerContactIds[0],
          prisonerContactRestrictions = listOf(
            createLocalRestriction(
              prisonerContactRestrictionId = 123L,
              prisonerContactId = prisonerContactIds[0],
              contactId = 2187524L,
              prisonerNumber = prisonerId,
              restrictionType = "CHILD",
              restrictionTypeDescription = "Banned",
              startDate = LocalDate.now().plusDays(5),
              expiryDate = LocalDate.now().plusDays(10),
            ),
          ),
          globalContactRestrictions = emptyList(),
        ),
        PrisonerContactRestrictionsDto(
          prisonerContactId = prisonerContactIds[1],
          prisonerContactRestrictions = listOf(
            createLocalRestriction(
              prisonerContactRestrictionId = 124L,
              prisonerContactId = prisonerContactIds[1],
              contactId = 2187525L,
              prisonerNumber = prisonerId,
              restrictionType = "CHILD",
              restrictionTypeDescription = "Banned",
              startDate = LocalDate.now(),
              expiryDate = LocalDate.now().plusDays(3),
            ),
          ),
          globalContactRestrictions = emptyList(),
        ),
      ),
    )

    personalRelationshipsApiMockServer.stubPrisonerContactRestrictions(
      prisonerContactIds = prisonerContactIds,
      response = restrictionResponse,
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().isOk
    val foundDateRanges = getResults(result)
    val expected = listOf(
      DateRangeDto(fromDate = LocalDate.now(), toDate = LocalDate.now().plusDays(3)),
      DateRangeDto(fromDate = LocalDate.now().plusDays(5), toDate = LocalDate.now().plusDays(10)),
    )

    Assertions.assertThat(foundDateRanges).containsExactlyInAnyOrderElementsOf(expected)
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - Unsupported restrictions filtered`() {
    // Given
    val prisonerId = "A1234AA"

    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = prisonerId,
      visitorIds = listOf("2187524", "2187525"),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    val prisonerContactIds = listOf(999001L, 999002L)
    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = listOf(2187524L, 2187525L),
      prisonerContactIds = prisonerContactIds,
      isApproved = true,
    )

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = prContacts,
      approvedVisitorOnly = true,
    )

    val restrictionResponse = PrisonerContactRestrictionsResponseDto(
      prisonerContactRestrictions = listOf(
        PrisonerContactRestrictionsDto(
          prisonerContactId = prisonerContactIds[0],
          prisonerContactRestrictions = listOf(
            createLocalRestriction(
              prisonerContactRestrictionId = 123L,
              prisonerContactId = prisonerContactIds[0],
              contactId = 2187524L,
              prisonerNumber = prisonerId,
              restrictionType = "CHILD",
              restrictionTypeDescription = "Banned",
              startDate = LocalDate.now().plusDays(5),
              expiryDate = LocalDate.now().plusDays(10),
            ),
          ),
          globalContactRestrictions = emptyList(),
        ),
        PrisonerContactRestrictionsDto(
          prisonerContactId = prisonerContactIds[1],
          prisonerContactRestrictions = listOf(
            createLocalRestriction(
              prisonerContactRestrictionId = 124L,
              prisonerContactId = prisonerContactIds[1],
              contactId = 2187525L,
              prisonerNumber = prisonerId,
              restrictionType = "UNSUPPORTED",
              restrictionTypeDescription = "Banned",
              startDate = LocalDate.now(),
              expiryDate = LocalDate.now().plusDays(3),
            ),
          ),
          globalContactRestrictions = emptyList(),
        ),
      ),
    )

    personalRelationshipsApiMockServer.stubPrisonerContactRestrictions(
      prisonerContactIds = prisonerContactIds,
      response = restrictionResponse,
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().isOk
    val foundDateRanges = getResults(result)
    val expected = listOf(
      DateRangeDto(fromDate = LocalDate.now().plusDays(5), toDate = LocalDate.now().plusDays(10)),
    )

    Assertions.assertThat(foundDateRanges).containsExactlyInAnyOrderElementsOf(expected)
  }

  private fun callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(
    webTestClient: WebTestClient,
    requestDto: RequestVisitVisitorRestrictionsBodyDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val uri = "v2/prisoners/${requestDto.prisonerId}/contacts/social/approved/restrictions/visit-request/date-ranges"

    return webTestClient.post().uri(uri)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(requestDto))
      .exchange()
  }

  private fun getResults(responseSpec: WebTestClient.ResponseSpec): List<DateRangeDto> = TestObjectMapper.mapper.readValue(responseSpec.expectBody().returnResult().responseBody, Array<DateRangeDto>::class.java).toList()
}
