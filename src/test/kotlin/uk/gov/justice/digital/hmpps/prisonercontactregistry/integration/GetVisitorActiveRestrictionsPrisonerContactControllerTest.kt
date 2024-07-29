package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.VisitorActiveRestrictionsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.enum.RestrictionType
import java.time.LocalDate

@Suppress("ClassName")
class GetVisitorActiveRestrictionsPrisonerContactControllerTest : IntegrationTestBase() {
  @SpyBean
  private lateinit var prisonApiClient: PrisonApiClient

  @Nested
  inner class authentication {
    @Test
    fun `requires authentication`() {
      // Given
      val prisonerId = "A1234AA"
      val visitorId: Long = 2187525
      val uri = createVisitorsActiveRestrictionUri(prisonerId, visitorId.toString())

      // When
      val result = webTestClient.get().uri(uri)
        .exchange()

      // Then
      result.expectStatus().isUnauthorized
    }

    @Test
    fun `Get visitor active restrictions requires correct role`() {
      // Given
      val prisonerId = "A1234AA"
      val visitorId: Long = 2187525
      val uri = createVisitorsActiveRestrictionUri(prisonerId, visitorId.toString())

      // When
      val result = webTestClient.get().uri(uri)
        .headers(setAuthorisation(roles = listOf("AnyThingWillDo")))
        .exchange()

      // Then
      result.expectStatus().isForbidden
      result.expectBody().jsonPath("userMessage").isEqualTo("Access denied")
    }

    @Test
    fun `Get visitor active restrictions requires correct role PRISONER_CONTACT_REGISTRY`() {
      // Given
      val prisonerId = "A1234AA"
      val visitorId: Long = 2187525
      val uri = createVisitorsActiveRestrictionUri(prisonerId, visitorId.toString())

      prisonApiMockServer.stubGetApprovedOffenderContacts(
        prisonerId,
        contacts = createContactsDto(restrictions = listOf(), listOf(visitorId)),
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
  fun `Get visitor active restrictions for visitorId not found within list of prisoner contacts`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorId: Long = 2187524
    val uri = createVisitorsActiveRestrictionUri(prisonerId, visitorId.toString())

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
      .isEqualTo("visitor provided ($visitorId) is not listed contact for prisoner $prisonerId")
  }

  @Test
  fun `All active restrictions returned if restrictions found with no expiry date for given visitors`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorId: Long = 2187524
    val uri = createVisitorsActiveRestrictionUri(prisonerId, visitorId.toString())

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
      contacts = createContactsDto(restrictions, listOf(visitorId)),
    )

    // When
    val result = webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // Then
    result.expectStatus().isOk

    val visitorActiveRestrictions = getVisitorActiveRestrictionDto(result)
    assertThat(visitorActiveRestrictions.activeRestrictions.size).isEqualTo(1)
    assertThat(visitorActiveRestrictions.activeRestrictions.first()).isEqualTo(RestrictionType.CLOSED.toString())
  }

  @Test
  fun `All active restrictions returned if restrictions found with expiry date in future for given visitors`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorId: Long = 2187524
    val uri = createVisitorsActiveRestrictionUri(prisonerId, visitorId.toString())

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
      contacts = createContactsDto(restrictions, listOf(visitorId)),
    )

    // When
    val result = webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // Then
    result.expectStatus().isOk

    val visitorActiveRestrictions = getVisitorActiveRestrictionDto(result)
    assertThat(visitorActiveRestrictions.activeRestrictions.size).isEqualTo(1)
    assertThat(visitorActiveRestrictions.activeRestrictions.first()).isEqualTo(RestrictionType.CLOSED.toString())
  }

  @Test
  fun `All active restrictions returned if restrictions found with expiry date of today for given visitors`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorId: Long = 2187524
    val uri = createVisitorsActiveRestrictionUri(prisonerId, visitorId.toString())

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
      contacts = createContactsDto(restrictions, listOf(visitorId)),
    )

    // When
    val result = webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // Then
    result.expectStatus().isOk

    val visitorActiveRestrictions = getVisitorActiveRestrictionDto(result)
    assertThat(visitorActiveRestrictions.activeRestrictions.size).isEqualTo(1)
    assertThat(visitorActiveRestrictions.activeRestrictions.first()).isEqualTo(RestrictionType.CLOSED.toString())
  }

  @Test
  fun `No active restrictions returned if restrictions found with expiry date in past`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorId: Long = 2187524
    val uri = createVisitorsActiveRestrictionUri(prisonerId, visitorId.toString())

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
      contacts = createContactsDto(restrictions, listOf(visitorId)),
    )

    // When
    val result = webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // Then
    result.expectStatus().isOk
    val visitorActiveRestrictions = getVisitorActiveRestrictionDto(result)
    assertThat(visitorActiveRestrictions.activeRestrictions.size).isEqualTo(0)
  }

  private fun createVisitorsActiveRestrictionUri(
    prisonerId: String,
    visitorId: String,
  ): String {
    return "/prisoners/$prisonerId/contacts/social/approved/$visitorId/restrictions/active"
  }

  fun getVisitorActiveRestrictionDto(responseSpec: ResponseSpec): VisitorActiveRestrictionsDto {
    return objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, VisitorActiveRestrictionsDto::class.java)
  }
}
