package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.v2

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonercontactregistry.controller.V2_PRISONER_GET_SOCIAL_RESTRICTION_CLOSED_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.enum.RestrictionType
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
      val visitorIds: List<Long> = listOf(2187525)
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
      val visitorIds: List<Long> = listOf(2187525)
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
      val visitorIds: List<Long> = listOf(2187525)
      val visitorIdsString = visitorIds.joinToString(",")
      val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

      prisonApiMockServer.stubGetApprovedOffenderContacts(
        prisonerId,
        contacts = createContactsDto(restrictions = listOf(), visitorIds),
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
    val visitorIds: List<Long> = listOf(2187524, 2187525)
    val visitorIdsString = visitorIds.joinToString(",")
    val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

    val contactWithMinimumDetails = ContactDto(
      lastName = "Ireron",
      firstName = "Ehicey",
      contactType = "S",
      relationshipCode = "PROB",
      relationshipDescription = "Probation Officer",
      commentText = "Comment Here",
      emergencyContact = false,
      nextOfKin = false,
      personId = 2187521,
      approvedVisitor = false,
      restrictions = listOf(),
    )

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      prisonerId,
      contacts = ContactsDto(listOf(contactWithMinimumDetails)),
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
    val visitorIdsString = visitorIds.joinToString(",")
    val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

    val restrictions = listOf(
      RestrictionDto(
        comment = "Comment Here",
        restrictionType = RestrictionType.CLOSED.toString(),
        restrictionTypeDescription = "CLOSED",
        startDate = LocalDate.of(2024, 1, 1),
        expiryDate = null,
        globalRestriction = false,
      ),
    )

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      prisonerId,
      contacts = createContactsDto(restrictions, visitorIds),
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
    val visitorIdsString = visitorIds.joinToString(",")
    val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

    val restrictions = listOf(
      RestrictionDto(
        comment = "Comment Here",
        restrictionType = RestrictionType.CLOSED.toString(),
        restrictionTypeDescription = "CLOSED",
        startDate = LocalDate.of(2024, 1, 1),
        expiryDate = LocalDate.now().plusDays(1),
        globalRestriction = false,
      ),
    )

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      prisonerId,
      contacts = createContactsDto(restrictions, visitorIds),
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
    val visitorIdsString = visitorIds.joinToString(",")
    val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

    val restrictions = listOf(
      RestrictionDto(
        comment = "Comment Here",
        restrictionType = RestrictionType.CLOSED.toString(),
        restrictionTypeDescription = "CLOSED",
        startDate = LocalDate.of(2024, 1, 1),
        expiryDate = LocalDate.now(),
        globalRestriction = false,
      ),
    )

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      prisonerId,
      contacts = createContactsDto(restrictions, visitorIds),
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
    val visitorIdsString = visitorIds.joinToString(",")
    val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

    val restrictions = listOf(
      RestrictionDto(
        comment = "Comment Here",
        restrictionType = RestrictionType.CLOSED.toString(),
        restrictionTypeDescription = "CLOSED",
        startDate = LocalDate.of(2024, 1, 1),
        expiryDate = LocalDate.now().minusDays(10),
        globalRestriction = false,
      ),
    )

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      prisonerId,
      contacts = createContactsDto(restrictions, visitorIds),
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
    val visitorIds: List<Long> = listOf(2187529L)
    val visitorIdsString = visitorIds.joinToString(",")
    val uri = createVisitorsClosedRestrictionUri(prisonerId, visitorIdsString)

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      prisonerId,
      contacts = createContactsDto(restrictions = listOf(), visitorIds),
    )

    // When
    val result = webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // Then
    result.expectStatus().isOk
    result.expectBody().jsonPath("$.value").isEqualTo(false)
  }

  private fun createVisitorsClosedRestrictionUri(
    prisonerId: String,
    visitorIdsString: String,
  ): String {
    return "v2/prisoners/$prisonerId/contacts/social/approved/restrictions/closed?visitors=$visitorIdsString"
  }
}
