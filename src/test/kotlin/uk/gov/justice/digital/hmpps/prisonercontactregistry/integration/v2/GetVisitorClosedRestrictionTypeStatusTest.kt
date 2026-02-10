package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.v2

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonercontactregistry.controller.V2_PRISONER_GET_SOCIAL_RESTRICTION_CLOSED_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.IntegrationTestBase
import java.time.LocalDate

@Suppress("ClassName")
@DisplayName("PrisonerContactControllerV2 - $V2_PRISONER_GET_SOCIAL_RESTRICTION_CLOSED_CONTROLLER_PATH")
class GetVisitorClosedRestrictionTypeStatusTest : IntegrationTestBase() {

  @Nested
  inner class authentication {
    @Test
    fun `requires authentication`() {
      // Given
      val prisonerId = "A1234AA"
      val visitorIds: List<Long> = listOf(2187525L)
      val visitorIdsString = visitorIds.joinToString(",")
      val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

      // When
      val result = webTestClient.get().uri(uri)
        .exchange()

      // Then
      result.expectStatus().isUnauthorized
    }

    @Test
    fun `Get visitor closed restriction status requires correct role`() {
      // Given
      val prisonerId = "A1234AA"
      val visitorIds: List<Long> = listOf(2187525L)
      val visitorIdsString = visitorIds.joinToString(",")
      val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

      // When
      val result = webTestClient.get().uri(uri)
        .headers(setAuthorisation(roles = listOf("AnyThingWillDo")))
        .exchange()

      // Then
      result.expectStatus().isForbidden
      result.expectBody().jsonPath("userMessage").isEqualTo("Access denied")
    }

    @Test
    fun `Get visitor closed restriction status requires correct role PRISONER_CONTACT_REGISTRY`() {
      // Given
      val prisonerId = "A1234AA"
      val visitorIds: List<Long> = listOf(2187525L)
      val visitorIdsString = visitorIds.joinToString(",")
      val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

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
  }

  @Test
  fun `Get visitor closed restriction status visitorId not found within list of prisoner contacts`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187524L, 2187525L)
    val visitorIdsString = visitorIds.joinToString(",")
    val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

    val returnedContactIds = listOf(2187521L)
    val returnedPrisonerContactIds = listOf(999001L)

    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = returnedContactIds,
      prisonerContactIds = returnedPrisonerContactIds,
      isApproved = true,
    )

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = prContacts,
      approvedVisitorOnly = true,
    )

    // Client will still call this after GET, so stub it as well (empty is fine)
    personalRelationshipsApiMockServer.stubPrisonerContactRestrictions(
      prisonerContactIds = returnedPrisonerContactIds,
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
  fun `Closed restriction status returned as 'true' if closed restriction found with no expiry date for given visitors`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187529L)
    val prisonerContactIds = listOf(999001L)
    val visitorIdsString = visitorIds.joinToString(",")
    val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = visitorIds,
      prisonerContactIds = prisonerContactIds,
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
              expiryDate = null,
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
    result.expectStatus().isOk
    result.expectBody().jsonPath("$.value").isEqualTo(true)
  }

  @Test
  fun `Closed restriction status returned as 'true' if closed restriction found with expiry date in future for given visitors`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187529L)
    val prisonerContactIds = listOf(999001L)
    val visitorIdsString = visitorIds.joinToString(",")
    val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = visitorIds,
      prisonerContactIds = prisonerContactIds,
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
              expiryDate = LocalDate.now().plusDays(10),
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
    result.expectStatus().isOk
    result.expectBody().jsonPath("$.value").isEqualTo(true)
  }

  @Test
  fun `Closed restriction status returned as 'true' if closed restriction found with expiry date of today for given visitors`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187529L)
    val prisonerContactIds = listOf(999001L)
    val visitorIdsString = visitorIds.joinToString(",")
    val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = visitorIds,
      prisonerContactIds = prisonerContactIds,
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
              expiryDate = LocalDate.now(),
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
    result.expectStatus().isOk
    result.expectBody().jsonPath("$.value").isEqualTo(true)
  }

  @Test
  fun `Closed restriction status returned as 'false' if closed restriction found with expiry date in past`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187529L)
    val prisonerContactIds = listOf(999001L)
    val visitorIdsString = visitorIds.joinToString(",")
    val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = visitorIds,
      prisonerContactIds = prisonerContactIds,
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
              expiryDate = LocalDate.now().minusDays(10),
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
    result.expectStatus().isOk
    result.expectBody().jsonPath("$.value").isEqualTo(false)
  }

  @Test
  fun `Closed restriction status returned as 'false' if no closed restriction found for visitors`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187525L)
    val prisonerContactIds = listOf(999001L)
    val visitorIdsString = visitorIds.joinToString(",")
    val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

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
    result.expectBody().jsonPath("$.value").isEqualTo(false)
  }

  @Test
  fun `Closed restriction endpoint should still process correctly even if duplicate visitorIds are found in contacts list`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187529L)
    val visitorIdsString = visitorIds.joinToString(",")
    val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

    // Duplicate visitorId/contactId returned twice (two relationships)
    val duplicateContactIds = listOf(2187529L, 2187529L)
    val prisonerContactIds = listOf(999001L, 999002L)

    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = duplicateContactIds,
      prisonerContactIds = prisonerContactIds,
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
              contactId = visitorIds[0],
              prisonerNumber = prisonerId,
              expiryDate = LocalDate.now().plusDays(10),
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
    result.expectStatus().isOk
    result.expectBody().jsonPath("$.value").isEqualTo(true)
  }

  private fun createVisitorsClosedRestrictionUri(
    prisonerId: String,
    visitorIdsString: String,
  ): String = "v2/prisoners/$prisonerId/contacts/social/approved/restrictions/closed?visitors=$visitorIdsString"
}
