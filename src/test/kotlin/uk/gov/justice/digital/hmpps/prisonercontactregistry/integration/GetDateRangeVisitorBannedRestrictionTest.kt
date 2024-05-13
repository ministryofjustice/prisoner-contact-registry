package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.mock.mockito.SpyBean
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.service.BANNED_RESTRICTION_TYPE
import java.time.LocalDate

@Suppress("ClassName")
class GetDateRangeVisitorBannedRestrictionTest : IntegrationTestBase() {
  @SpyBean
  private lateinit var prisonApiClient: PrisonApiClient

  @Nested
  inner class authentication {
    @Test
    fun `Get date range for visitors with banned restrictions requires authentication`() {
      // Given
      val prisonerId = "A1234AA"
      val visitorIds: List<Long> = listOf(2187525)
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
      val visitorIds: List<Long> = listOf(2187525)
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
      val visitorIds: List<Long> = listOf(2187525)
      val visitorIdsString = visitorIds.joinToString(",")
      val fromDate: LocalDate = LocalDate.now()
      val toDate: LocalDate = LocalDate.now()
      val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

      prisonApiMockServer.stubGetApprovedOffenderContacts(
        prisonerId,
        contacts = createContactsDto(restrictions = listOf(), visitorIds)
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
    val visitorIds: List<Long> = listOf(2187524, 2187525)
    val visitorIdsString = visitorIds.joinToString(",")
    val fromDate: LocalDate = LocalDate.now().minusDays(2)
    val toDate: LocalDate = LocalDate.now().minusDays(2)
    val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

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
  fun `Get date range for visitors with banned restrictions No applicable date range found due to visitor having open ended BAN restriction`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187529L, 2187526)
    val visitorIdsString = visitorIds.joinToString(",")
    val fromDate: LocalDate = LocalDate.now().minusDays(2)
    val toDate: LocalDate = LocalDate.now().plusDays(2)
    val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

    val restrictions = listOf(
      RestrictionDto(
        comment = "Comment Here",
        restrictionType = BANNED_RESTRICTION_TYPE,
        restrictionTypeDescription = "Banned",
        startDate = fromDate,
        expiryDate = null,
        globalRestriction = false,
      ),
      RestrictionDto(
        comment = "Comment Here",
        restrictionType = BANNED_RESTRICTION_TYPE,
        restrictionTypeDescription = "Banned",
        startDate = fromDate,
        expiryDate = toDate.plusDays(1),
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
    val visitorIds: List<Long> = listOf(2187526)
    val visitorIdsString = visitorIds.joinToString(",")
    val fromDate: LocalDate = LocalDate.of(2024, 5, 1)
    val toDate: LocalDate = LocalDate.of(2024, 5, 10)
    val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

    val restrictions = listOf(
      RestrictionDto(
        comment = "Comment Here",
        restrictionType = BANNED_RESTRICTION_TYPE,
        restrictionTypeDescription = "Banned",
        startDate = fromDate,
        expiryDate = toDate.plusDays(1),
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
    val visitorId = 2187529L
    val visitorIds: List<Long> = listOf(visitorId)
    val visitorIdsString = visitorIds.joinToString(",")
    val fromDate: LocalDate = LocalDate.of(2024, 5, 9)
    val toDate: LocalDate = LocalDate.of(2024, 5, 10)
    val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

    val restrictions = listOf(
      RestrictionDto(
        comment = "Comment Here",
        restrictionType = BANNED_RESTRICTION_TYPE,
        restrictionTypeDescription = "Banned",
        startDate = fromDate,
        expiryDate = toDate,
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
    val visitorIds: List<Long> = listOf(2187525)
    val visitorIdsString = visitorIds.joinToString(",")
    val fromDate: LocalDate = LocalDate.now()
    val toDate: LocalDate = LocalDate.now()
    val uri = createDateRangeBanUri(prisonerId, visitorIdsString, fromDate, toDate)

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

  private fun createDateRangeBanUri(
    prisonerId: String,
    visitorIdsString: String,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): String {
    return "/prisoners/$prisonerId/approved/social/contacts/restrictions/banned/dateRange?visitors=$visitorIdsString&fromDate=$fromDate&toDate=$toDate"
  }
}
