package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.controller.V2_GET_PRISONER_CONTACT_RELATIONSHIP_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.TestObjectMapper
import java.time.LocalDate

@Suppress("ClassName")
@DisplayName("PrisonerContactControllerV2 - $V2_GET_PRISONER_CONTACT_RELATIONSHIP_CONTROLLER_PATH")
class PrisonerGetContactViaRelationshipIdTest : IntegrationTestBase() {
  @Nested
  inner class authentication {
    @Test
    fun `requires authentication`() {
      val prisonerId = "A1234AA"
      val contactId = 2187525L
      val relationshipId = 999001L

      webTestClient.get().uri("v2/prisoners/$prisonerId/contacts/$contactId/relationships/$relationshipId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      val prisonerId = "A1234AA"
      val contactId = 2187525L
      val relationshipId = 999001L

      webTestClient.get().uri("v2/prisoners/$prisonerId/contacts/$contactId/relationships/$relationshipId")
        .headers(setAuthorisation(roles = listOf("AnyThingWillDo")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Access denied")
    }
  }

  @Test
  fun `when call to get prisoner contact details via relationship is made (withRestrictions true) then contact details are returned with restrictions`() {
    val prisonerId = "A1234AA"
    val chosenContact = 2187525L
    val otherContact = 2187526L

    val contactIds = listOf(chosenContact, otherContact)

    val relationshipIds = listOf(999001L, 999002L)

    val prContacts = mutableListOf<PersonalRelationshipsContactDto>()

    prContacts.addAll(
      createPersonalRelationshipsContactDtoList(
        contactIds = contactIds,
        prisonerContactIds = relationshipIds,
      ),
    )

    personalRelationshipsApiMockServer.stubGetPrisonerContactViaRelationshipId(
      prisonerId = prisonerId,
      contactId = chosenContact,
      relationships = prContacts,
    )

    val restrictionResponse = PrisonerContactRestrictionsResponseDto(
      prisonerContactRestrictions = listOf(
        PrisonerContactRestrictionsDto(
          prisonerContactId = 999001L,
          prisonerContactRestrictions = listOf(
            createLocalRestriction(
              prisonerContactRestrictionId = 123L,
              prisonerContactId = 999001L,
              contactId = chosenContact,
              prisonerNumber = prisonerId,
              expiryDate = null,
            ),
          ),
          globalContactRestrictions = emptyList(),
        ),
      ),
    )

    personalRelationshipsApiMockServer.stubPrisonerContactRestrictions(
      prisonerContactIds = listOf(999001L),
      response = restrictionResponse,
    )

    val returnResult = callGetPrisonerContactViaRelationshipId(
      prisonerId = prisonerId,
      contactId = chosenContact,
      relationshipId = 999001L,
      withRestrictions = true,
    )
      .expectStatus().isOk
      .expectBody()

    val contact = getResults(returnResult)

    assertThat(contact.personId).isEqualTo(chosenContact)
    assertContactAddress(contact.address!!)
    assertThat(contact.restrictions.size).isEqualTo(1)

    verify(personalRelationshipsApiClientSpy, times(1)).getPrisonerContactViaRelationshipId(prisonerId = prisonerId, contactId = chosenContact.toString(), relationshipId = 999001L, withRestrictions = true)
  }

  @Test
  fun `when call to get prisoner contact details via relationship is made (withRestrictions false) then contact details are returned without restrictions`() {
    val prisonerId = "A1234AA"
    val chosenContact = 2187525L
    val otherContact = 2187526L

    val contactIds = listOf(chosenContact, otherContact)

    val relationshipIds = listOf(999001L, 999002L)

    val prContacts = mutableListOf<PersonalRelationshipsContactDto>()

    prContacts.addAll(
      createPersonalRelationshipsContactDtoList(
        contactIds = contactIds,
        prisonerContactIds = relationshipIds,
      ),
    )

    personalRelationshipsApiMockServer.stubGetPrisonerContactViaRelationshipId(
      prisonerId = prisonerId,
      contactId = chosenContact,
      relationships = prContacts,
    )

    val returnResult = callGetPrisonerContactViaRelationshipId(
      prisonerId = prisonerId,
      contactId = chosenContact,
      relationshipId = 999001L,
      withRestrictions = false,
      )
      .expectStatus().isOk
      .expectBody()

    val contact = getResults(returnResult)

    assertThat(contact.personId).isEqualTo(chosenContact)
    assertContactAddress(contact.address!!)
    assertThat(contact.restrictions).isEmpty()

    verify(personalRelationshipsApiClientSpy, times(1)).getPrisonerContactViaRelationshipId(prisonerId = prisonerId, contactId = chosenContact.toString(), relationshipId = 999001L, withRestrictions = false)
  }

  @Test
  fun `when personal relationship API restrictions call fails with BAD_REQUEST then entire call fails`() {
    // Given
    val prisonerId = "A1234AA"
    val contactId = 2187525L
    val relationshipId = 999001L

    val prContacts = createPersonalRelationshipsContactDtoList(
      contactIds = listOf(contactId),
      prisonerContactIds = listOf(relationshipId),
    )

    personalRelationshipsApiMockServer.stubGetPrisonerContactViaRelationshipId(
      prisonerId = prisonerId,
      contactId = contactId,
      relationships = prContacts,
      httpStatus = HttpStatus.BAD_REQUEST,
    )

    // When
    val result = webTestClient.get().uri("v2/prisoners/$prisonerId/contacts/$contactId/relationships/$relationshipId")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // Then
    result.expectStatus().isBadRequest
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): ContactDto = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, ContactDto::class.java)

  private fun callGetPrisonerContactViaRelationshipId(
    prisonerId: String,
    contactId: Long,
    relationshipId: Long,
    withRestrictions: Boolean? = null,
  ): WebTestClient.ResponseSpec {
    val queryParams = mutableListOf<String>()

    withRestrictions?.let { queryParams.add("withRestrictions=$it") }

    val uri = if (queryParams.isEmpty()) {
      "v2/prisoners/$prisonerId/contacts/$contactId/relationships/$relationshipId"
    } else {
      "v2/prisoners/$prisonerId/contacts/$contactId/relationships/$relationshipId?${queryParams.joinToString("&")}"
    }

    return webTestClient
      .get()
      .uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
  }
}
