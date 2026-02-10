package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.v2

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonercontactregistry.controller.V2_PRISONER_GET_SOCIAL_RESTRICTION_BANNED_DATE_RANGE_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.enum.RestrictionType
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.IntegrationTestBase
import java.time.LocalDate

@Suppress("ClassName")
@DisplayName("PrisonerContactControllerV2 - $V2_PRISONER_GET_SOCIAL_RESTRICTION_BANNED_DATE_RANGE_CONTROLLER_PATH")
class GetDateRangeVisitorBannedRestrictionTypeTest : IntegrationTestBase() {
  @Nested
  inner class authentication {
    @Test
    fun `Get date range for visitors with banned restrictions requires authentication`() {
      // Given
      val prisonerId = "A1234AA"
      val visitorIds: List<Long> = listOf(2187525L)
      val visitorIdsString = visitorIds.joinToString(",")
      val fromDate: LocalDate = LocalDate.now()
      val toDate: LocalDate = LocalDate.now()
      val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

      // When
      val result = webTestClient.get().uri(uri)
        .exchange()

      // Then
      result.expectStatus().isUnauthorized
    }

    @Test
    fun `Get date range for visitors with banned restrictions requires correct role`() {
      // Given
      val prisonerId = "A1234AA"
      val visitorIds: List<Long> = listOf(2187525L)
      val visitorIdsString = visitorIds.joinToString(",")
      val fromDate: LocalDate = LocalDate.now()
      val toDate: LocalDate = LocalDate.now()
      val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

      // When
      val result = webTestClient.get().uri(uri)
        .headers(setAuthorisation(roles = listOf("AnyThingWillDo")))
        .exchange()

      // Then
      result.expectStatus().isForbidden
      result.expectBody().jsonPath("userMessage").isEqualTo("Access denied")
    }

    @Test
    fun `Get date range for visitors with banned restrictions requires correct role PRISONER_CONTACT_REGISTRY`() {
      // Given
      val prisonerId = "A1234AA"
      val visitorIds: List<Long> = listOf(2187525L)
      val visitorIdsString = visitorIds.joinToString(",")
      val fromDate: LocalDate = LocalDate.now()
      val toDate: LocalDate = LocalDate.now()
      val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

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
      val result = webTestClient.get().uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
        .exchange()

      // Then
      result.expectStatus().isOk
    }
  }

  @Test
  fun `Get date range for visitors with banned restrictions bad request`() {
    // Given
    val prisonerId = "A1234AA"
    val wrongVisitorId = "badRequest"
    val fromDate: LocalDate = LocalDate.now().minusDays(2)
    val toDate: LocalDate = LocalDate.now().minusDays(2)
    val uri = createDateRangeBanUri(prisonerId, wrongVisitorId, fromDate, toDate)

    // When
    val result = webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // Then
    result.expectStatus().isBadRequest
  }

  @Test
  fun `Get date range for visitors with banned restrictions visitorId not found within list of prisoner contacts`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187524L, 2187525L)
    val visitorIdsString = visitorIds.joinToString(",")
    val fromDate: LocalDate = LocalDate.now().minusDays(2)
    val toDate: LocalDate = LocalDate.now().minusDays(2)
    val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

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
    val result = webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // Then
    result.expectStatus().isNotFound
    result.expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("One of the visitors provided could not found")
      .jsonPath("$.developerMessage")
      .isEqualTo("Not all visitors provided ($visitorIds) are listed contacts for prisoner $prisonerId")
  }

  @Test
  fun `Get date range for visitors with banned restrictions No applicable date range found due to visitor having open ended BAN restriction`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187529L, 2187526L)
    val visitorIdsString = visitorIds.joinToString(",")
    val fromDate: LocalDate = LocalDate.now().minusDays(2)
    val toDate: LocalDate = LocalDate.now().plusDays(2)
    val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

    val prisonerContactIds = listOf(999001L, 999002L)
    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = visitorIds,
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
              contactId = visitorIds[0],
              prisonerNumber = prisonerId,
              restrictionType = RestrictionType.BANNED.toString(),
              restrictionTypeDescription = "Banned",
              startDate = fromDate,
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
              contactId = visitorIds[1],
              prisonerNumber = prisonerId,
              restrictionType = RestrictionType.BANNED.toString(),
              restrictionTypeDescription = "Banned",
              startDate = fromDate,
              expiryDate = toDate.plusDays(1),
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
    val result = webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // Then
    result.expectStatus().isNotFound
    result.expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("One of the visitors provided has a BAN restriction, no suitable date range found")
      .jsonPath("$.developerMessage")
      .isEqualTo("Found visitor with restriction of 'BAN' with no expiry date, no date range possible")
  }

  @Test
  fun `Get date range for visitors with banned restrictions No applicable date range found due to visitor having BAN restriction expiring after our endDate`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187526L)
    val visitorIdsString = visitorIds.joinToString(",")
    val fromDate: LocalDate = LocalDate.of(2024, 5, 1)
    val toDate: LocalDate = LocalDate.of(2024, 5, 10)
    val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

    val prisonerContactIds = listOf(999001L)
    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = visitorIds,
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
              contactId = visitorIds[0],
              prisonerNumber = prisonerId,
              restrictionType = RestrictionType.BANNED.toString(),
              restrictionTypeDescription = "Banned",
              startDate = fromDate,
              expiryDate = toDate.plusDays(1),
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
    val result = webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // Then
    result.expectStatus().isNotFound
    result.expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("One of the visitors provided has a BAN restriction, no suitable date range found")
      .jsonPath("$.developerMessage")
      .isEqualTo("Found visitor with restriction of 'BAN' with expiry date after our endDate, no date range possible")
  }

  @Test
  fun `Get date range for visitors with banned restrictions No applicable date range found due to visitor having BAN restriction expiring on our endDate`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187529L)
    val visitorIdsString = visitorIds.joinToString(",")
    val fromDate: LocalDate = LocalDate.of(2024, 5, 9)
    val toDate: LocalDate = LocalDate.of(2024, 5, 10)
    val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

    val prisonerContactIds = listOf(999001L)
    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = visitorIds,
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
              contactId = visitorIds[0],
              prisonerNumber = prisonerId,
              restrictionType = RestrictionType.BANNED.toString(),
              restrictionTypeDescription = "Banned",
              startDate = fromDate,
              expiryDate = toDate,
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
    val result = webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // Then
    result.expectStatus().isNotFound
    result.expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("One of the visitors provided has a BAN restriction, no suitable date range found")
      .jsonPath("$.developerMessage")
      .isEqualTo("Found visitor with restriction of 'BAN' with expiry date after our endDate, no date range possible")
  }

  @Test
  fun `Get date range for visitors with banned restrictions Date range is returned successfully when visitors have no BAN restrictions`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187525L)
    val visitorIdsString = visitorIds.joinToString(",")
    val fromDate: LocalDate = LocalDate.now()
    val toDate: LocalDate = LocalDate.now()
    val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

    val prisonerContactIds = listOf(999001L)
    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = visitorIds,
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
    val result = webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // Then
    result.expectStatus().isOk
  }

  private fun createDateRangeBanUri(
    prisonerId: String,
    visitorIdsString: String,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): String = "v2/prisoners/$prisonerId/contacts/social/approved/restrictions/banned/dateRange?visitors=$visitorIdsString&fromDate=$fromDate&toDate=$toDate"
}
