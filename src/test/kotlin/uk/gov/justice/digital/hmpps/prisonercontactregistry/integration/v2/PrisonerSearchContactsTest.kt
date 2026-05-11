package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.controller.V2_PRISONER_SEARCH_CONTACTS
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactWithOptionalPrisonerRelationshipDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.TestObjectMapper
import java.time.LocalDate

@Suppress("ClassName")
@DisplayName("PrisonerContactController - $V2_PRISONER_SEARCH_CONTACTS")
class PrisonerSearchContactsTest : IntegrationTestBase() {
  @Nested
  inner class authentication {
    @Test
    fun `requires authentication`() {
      val prisonerId = "A1234AA"
      webTestClient.get().uri("/v2/prisoners/$prisonerId/contacts/search")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      val prisonerId = "A1234AA"
      val contactIds = listOf(2187524L, 3147515L)

      webTestClient.get()
        .uri("/v2/prisoners/$prisonerId/contacts/search?contactIds=${contactIds.joinToString(",")}")
        .headers(setAuthorisation(roles = listOf("AnyThingWillDo")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Access denied")
    }
  }

  @Test
  fun `search contacts returns contact with prisoner relationship and restrictions`() {
    // Given
    val prisonerId = "A1234AA"
    val contactIds = listOf(2187524L)
    val prisonerContactIds = listOf(999001L)

    val contacts = createPersonalRelationshipsContactSearchResultDtoList(
      contactIds = contactIds,
      prisonerContactIds = prisonerContactIds,
    )

    val restrictionResponse = PrisonerContactRestrictionsResponseDto(
      prisonerContactRestrictions = listOf(
        PrisonerContactRestrictionsDto(
          prisonerContactId = prisonerContactIds[0],
          prisonerContactRestrictions = listOf(
            createLocalRestriction(
              prisonerContactRestrictionId = 123L,
              prisonerContactId = prisonerContactIds[0],
              contactId = contactIds[0],
              prisonerNumber = prisonerId,
              restrictionType = "CHILD",
              restrictionTypeDescription = "Banned",
              startDate = LocalDate.now().plusDays(5),
              expiryDate = null,
            ),
          ),
          globalContactRestrictions = emptyList(),
        ),
      ),
    )

    personalRelationshipsApiMockServer.stubSearchContacts(
      prisonerId = prisonerId,
      contactIds = contactIds,
      contacts = contacts,
    )

    personalRelationshipsApiMockServer.stubPrisonerContactRestrictions(
      prisonerContactIds = prisonerContactIds,
      response = restrictionResponse,
    )

    // When
    val result = callSearchContacts(
      prisonerId = prisonerId,
      contactIds = contactIds,
      withRestrictions = true,
    )

    // Then
    result.expectStatus().isOk

    val responseList = getContactResults(result.expectBody())
    assertThat(responseList).hasSize(1)
    assertThat(responseList[0].contactId).isEqualTo(2187524)
    assertThat(responseList[0].prisonerContactId).isEqualTo(999001L)
    assertThat(responseList[0].restrictions).isNotEmpty()

    verify(personalRelationshipsApiClientSpy, times(1)).searchContact(prisonerId, contactIds)
    verify(personalRelationshipsApiClientSpy, times(1)).getPrisonerContactRestrictions(prisonerContactIds)
    verify(personalRelationshipsApiClientSpy, never()).getContactGlobalRestrictions(any())
    verifyNoMoreInteractions(personalRelationshipsApiClientSpy)
  }

  @Test
  fun `search contacts returns contact with global restrictions only when no prisoner relationship exists`() {
    // Given
    val prisonerId = "A1234AA"
    val contactIds = listOf(2187524L)
    val prisonerContactIds = listOf<Long?>(null)

    val contacts = createPersonalRelationshipsContactSearchResultDtoList(
      contactIds = contactIds,
      prisonerContactIds = prisonerContactIds,
    )

    val globalRestrictions = listOf(
      createGlobalRestriction(
        contactRestrictionId = 123L,
        contactId = contactIds[0],
        restrictionType = "CHILD",
        restrictionTypeDescription = "Banned",
        startDate = LocalDate.now().plusDays(5),
        expiryDate = null,
      ),
    )

    personalRelationshipsApiMockServer.stubSearchContacts(
      prisonerId = prisonerId,
      contactIds = contactIds,
      contacts = contacts,
    )

    personalRelationshipsApiMockServer.stubGetContactGlobalRestrictions(
      contactId = contactIds[0],
      restrictions = globalRestrictions,
    )

    // When
    val result = callSearchContacts(
      prisonerId = prisonerId,
      contactIds = contactIds,
      withRestrictions = true,
    )

    // Then
    result.expectStatus().isOk

    val responseList = getContactResults(result.expectBody())
    assertThat(responseList).hasSize(1)
    assertThat(responseList[0].contactId).isEqualTo(2187524)
    assertThat(responseList[0].prisonerContactId).isNull()
    assertThat(responseList[0].restrictions).isNotEmpty()
    assertThat(responseList[0].restrictions).hasSize(1)
    assertThat(responseList[0].restrictions[0].restrictionType).isEqualTo("CHILD")
    assertThat(responseList[0].restrictions[0].globalRestriction).isTrue()

    verify(personalRelationshipsApiClientSpy, times(1)).searchContact(prisonerId, contactIds)
    verify(personalRelationshipsApiClientSpy, never()).getPrisonerContactRestrictions(any())
    verify(personalRelationshipsApiClientSpy, times(1)).getContactGlobalRestrictions(contactIds[0])
    verifyNoMoreInteractions(personalRelationshipsApiClientSpy)
  }

  @Test
  fun `search contacts returns relationship restrictions and global restrictions for mixed relationship results`() {
    // Given
    val prisonerId = "A1234AA"
    val contactIds = listOf(2187524L, 2187525L)
    val prisonerContactIds = listOf<Long?>(999001L, null)

    val contacts = createPersonalRelationshipsContactSearchResultDtoList(
      contactIds = contactIds,
      prisonerContactIds = prisonerContactIds,
    )

    val prisonerContactRestrictionResponse = PrisonerContactRestrictionsResponseDto(
      prisonerContactRestrictions = listOf(
        PrisonerContactRestrictionsDto(
          prisonerContactId = 999001L,
          prisonerContactRestrictions = listOf(
            createLocalRestriction(
              prisonerContactRestrictionId = 123L,
              prisonerContactId = 999001L,
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
      ),
    )

    val globalRestrictions = listOf(
      createGlobalRestriction(
        contactRestrictionId = 456L,
        contactId = 2187525L,
        restrictionType = "CLOSED",
        restrictionTypeDescription = "Closed",
        startDate = LocalDate.now().plusDays(10),
        expiryDate = null,
      ),
    )

    personalRelationshipsApiMockServer.stubSearchContacts(
      prisonerId = prisonerId,
      contactIds = contactIds,
      contacts = contacts,
    )

    personalRelationshipsApiMockServer.stubPrisonerContactRestrictions(
      prisonerContactIds = listOf(999001L),
      response = prisonerContactRestrictionResponse,
    )

    personalRelationshipsApiMockServer.stubGetContactGlobalRestrictions(
      contactId = 2187525L,
      restrictions = globalRestrictions,
    )

    // When
    val result = callSearchContacts(
      prisonerId = prisonerId,
      contactIds = contactIds,
      withRestrictions = true,
    )

    // Then
    result.expectStatus().isOk

    val responseList = getContactResults(result.expectBody())
    assertThat(responseList).hasSize(2)

    val contactWithRelationship = responseList.first { it.contactId == 2187524L }
    assertThat(contactWithRelationship.prisonerContactId).isEqualTo(999001L)
    assertThat(contactWithRelationship.restrictions).hasSize(1)
    assertThat(contactWithRelationship.restrictions[0].restrictionType).isEqualTo("CHILD")
    assertThat(contactWithRelationship.restrictions[0].globalRestriction).isFalse()

    val contactWithoutRelationship = responseList.first { it.contactId == 2187525L }
    assertThat(contactWithoutRelationship.prisonerContactId).isNull()
    assertThat(contactWithoutRelationship.restrictions).hasSize(1)
    assertThat(contactWithoutRelationship.restrictions[0].restrictionType).isEqualTo("CLOSED")
    assertThat(contactWithoutRelationship.restrictions[0].globalRestriction).isTrue()

    verify(personalRelationshipsApiClientSpy, times(1)).searchContact(prisonerId, contactIds)
    verify(personalRelationshipsApiClientSpy, times(1)).getPrisonerContactRestrictions(listOf(999001L))
    verify(personalRelationshipsApiClientSpy, times(1)).getContactGlobalRestrictions(2187525L)
    verifyNoMoreInteractions(personalRelationshipsApiClientSpy)
  }

  @Test
  fun `search contacts returns contact with prisoner relationship but no restriction when withRestrictions is false`() {
    // Given
    val prisonerId = "A1234AA"
    val contactIds = listOf(2187524L)
    val prisonerContactIds = listOf(999001L)

    val contacts = createPersonalRelationshipsContactSearchResultDtoList(
      contactIds = contactIds,
      prisonerContactIds = prisonerContactIds,
    )

    personalRelationshipsApiMockServer.stubSearchContacts(
      prisonerId = prisonerId,
      contactIds = contactIds,
      contacts = contacts,
    )

    // When
    val result = callSearchContacts(
      prisonerId = prisonerId,
      contactIds = contactIds,
      withRestrictions = false,
    )

    // Then
    result.expectStatus().isOk

    val responseList = getContactResults(result.expectBody())
    assertThat(responseList).hasSize(1)
    assertThat(responseList[0].contactId).isEqualTo(2187524)
    assertThat(responseList[0].prisonerContactId).isEqualTo(999001L)
    assertThat(responseList[0].restrictions).isEmpty()

    verify(personalRelationshipsApiClientSpy, times(1)).searchContact(prisonerId, contactIds)
    verify(personalRelationshipsApiClientSpy, never()).getPrisonerContactRestrictions(any())
    verify(personalRelationshipsApiClientSpy, never()).getContactGlobalRestrictions(any())
    verifyNoMoreInteractions(personalRelationshipsApiClientSpy)
  }

  @Test
  fun `search contacts returns bad request when personal relationships search returns bad request`() {
    // Given
    val prisonerId = "A1234AA"
    val contactIds = listOf(2187524L)

    personalRelationshipsApiMockServer.stubSearchContacts(
      prisonerId = prisonerId,
      contactIds = contactIds,
      contacts = null,
      httpStatus = HttpStatus.BAD_REQUEST,
    )

    // When
    val result = callSearchContacts(
      prisonerId = prisonerId,
      contactIds = contactIds,
    )

    // Then
    result.expectStatus().isBadRequest

    verify(personalRelationshipsApiClientSpy, times(1)).searchContact(prisonerId, contactIds)
    verify(personalRelationshipsApiClientSpy, never()).getPrisonerContactRestrictions(any())
    verify(personalRelationshipsApiClientSpy, never()).getContactGlobalRestrictions(any())
    verifyNoMoreInteractions(personalRelationshipsApiClientSpy)
  }

  private fun getContactResults(returnResult: WebTestClient.BodyContentSpec): Array<ContactWithOptionalPrisonerRelationshipDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<ContactWithOptionalPrisonerRelationshipDto>::class.java)

  private fun callSearchContacts(
    prisonerId: String,
    contactIds: List<Long>,
    withRestrictions: Boolean? = null,
  ): WebTestClient.ResponseSpec {
    val queryParams = mutableListOf<String>()

    queryParams.add("contactIds=${contactIds.joinToString(",")}")

    withRestrictions?.let { queryParams.add("withRestrictions=$it") }

    val uri = "/v2/prisoners/$prisonerId/contacts/search?${queryParams.joinToString("&")}"

    return webTestClient
      .get()
      .uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
  }
}
