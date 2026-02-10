package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonercontactregistry.controller.V2_PRISONER_GET_SOCIAL_CONTACTS_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.TestObjectMapper
import java.time.LocalDate

@Suppress("ClassName")
@DisplayName("PrisonerContactControllerV2 - $V2_PRISONER_GET_SOCIAL_CONTACTS_CONTROLLER_PATH")
class PrisonerGetSocialContactsTest : IntegrationTestBase() {
  @MockitoSpyBean
  private lateinit var prisonApiClientSpy: PrisonApiClient

  @MockitoSpyBean
  private lateinit var personalRelationshipsApiClient: PersonalRelationshipsApiClient

  val socialContactWithRestrictionId = 1L
  val socialContactWithExpiredRestrictionId = 2L
  val socialContactWithNoDOB = 3L

  @Nested
  inner class authentication {
    @Test
    fun `requires authentication`() {
      val prisonerId = "A1234AA"
      webTestClient.get().uri("v2/prisoners/$prisonerId/contacts/social")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      val prisonerId = "A1234AA"
      webTestClient.get().uri("v2/prisoners/$prisonerId/contacts/social")
        .headers(setAuthorisation(roles = listOf("AnyThingWillDo")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Access denied")
    }

    @Test
    fun `requires correct role PRISONER_CONTACT_REGISTRY`() {
      // Given
      val prisonerId = "A1234AA"
      val visitorIds: List<Long> = listOf(2187525)

      val prisonerContactIds = listOf(999001L)
      val prContacts = createPersonalRelationshipsContactsDto(
        contactIds = visitorIds,
        prisonerContactIds = prisonerContactIds,
        isApproved = false,
      )

      personalRelationshipsApiMockServer.stubGetAllContacts(
        prisonerId = prisonerId,
        contacts = prContacts,
        approvedVisitorOnly = false,
      )

      personalRelationshipsApiMockServer.stubPrisonerContactRestrictions(
        prisonerContactIds = prisonerContactIds,
      )

      webTestClient.get().uri("v2/prisoners/$prisonerId/contacts/social")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @Test
  fun `when withAddress is false then no call is made to get address`() {
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(socialContactWithRestrictionId, socialContactWithExpiredRestrictionId)
    val prisonerContactIds = listOf(999001L, 999002L)

    val prContacts = createPersonalRelationshipsContactsDto(
      contactIds = visitorIds,
      prisonerContactIds = prisonerContactIds,
      isApproved = false,
    )

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = prContacts,
      approvedVisitorOnly = false,
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
              expiryDate = LocalDate.now().minusDays(1),
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

    prisonApiMockServer.stubGetPersonAddressesFullAddress(visitorIds[0], createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(visitorIds[1], createContactsAddressDto())

    val returnResult = callGetSocialContacts(prisonerId, withAddress = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)

    assertThat(contacts).hasSize(2)
    assertThat(contacts.map { it.personId }).containsExactlyInAnyOrder(visitorIds[0], visitorIds[1])
    assertThat(contacts).allSatisfy { contact -> assertThat(contact.addresses).isEmpty() }

    verify(personalRelationshipsApiClient, times(1)).getPrisonerContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
  }

  @Test
  fun `when hasDateOfBirth is passed as true only social contacts with a DOB are returned`() {
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(socialContactWithRestrictionId, socialContactWithExpiredRestrictionId)
    val prisonerContactIds = listOf(999001L, 999002L)

    val prContacts = mutableListOf<PersonalRelationshipsContactDto>()

    prContacts.addAll(
      createPersonalRelationshipsContactsDto(
        contactIds = visitorIds,
        prisonerContactIds = prisonerContactIds,
        isApproved = false,
      ),
    )

    prContacts.add(
      PersonalRelationshipsContactDto(
        contactId = socialContactWithNoDOB,
        prisonerContactId = 999003L,
        firstName = "test",
        middleNames = "middle",
        lastName = "user",
        dateOfBirth = null, // No D.O.B
        relationshipToPrisonerCode = "FRI",
        relationshipToPrisonerDescription = "Friend",
        relationshipTypeCode = "S",
        relationshipTypeDescription = "Social",
        isApprovedVisitor = false,
        isEmergencyContact = false,
        isNextOfKin = false,
        comments = "Comment Here",
      ),
    )

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = prContacts,
      approvedVisitorOnly = false,
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
              expiryDate = LocalDate.now().minusDays(1),
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

    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithRestrictionId, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithExpiredRestrictionId, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithNoDOB, createContactsAddressDto())

    val returnResult = callGetSocialContacts(prisonerId, hasDateOfBirth = true)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)

    assertThat(contacts).hasSize(2)
    assertThat(contacts.map { it.personId }).containsExactlyInAnyOrder(visitorIds[0], visitorIds[1])

    contacts.forEach { contact ->
      assertThat(contact.addresses).hasSize(1)
      assertContactAddress(contact.addresses[0])
    }

    verify(personalRelationshipsApiClient, times(1)).getPrisonerContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithRestrictionId)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithExpiredRestrictionId)
  }

  @Test
  fun `when hasDateOfBirth is passed as false all social contacts with or without a DOB are returned`() {
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(socialContactWithRestrictionId, socialContactWithExpiredRestrictionId)
    val prisonerContactIds = listOf(999001L, 999002L)

    val prContacts = mutableListOf<PersonalRelationshipsContactDto>()

    prContacts.addAll(
      createPersonalRelationshipsContactsDto(
        contactIds = visitorIds,
        prisonerContactIds = prisonerContactIds,
        isApproved = false,
      ),
    )

    prContacts.add(
      PersonalRelationshipsContactDto(
        contactId = socialContactWithNoDOB,
        prisonerContactId = 999003L,
        firstName = "test",
        middleNames = "middle",
        lastName = "user",
        dateOfBirth = null, // No D.O.B
        relationshipToPrisonerCode = "FRI",
        relationshipToPrisonerDescription = "Friend",
        relationshipTypeCode = "S",
        relationshipTypeDescription = "Social",
        isApprovedVisitor = false,
        isEmergencyContact = false,
        isNextOfKin = false,
        comments = "Comment Here",
      ),
    )

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = prContacts,
      approvedVisitorOnly = false,
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
              expiryDate = LocalDate.now().minusDays(1),
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

    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithRestrictionId, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithExpiredRestrictionId, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithNoDOB, createContactsAddressDto())

    val returnResult = callGetSocialContacts(prisonerId, hasDateOfBirth = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)

    assertThat(contacts).hasSize(3)
    assertThat(contacts.map { it.personId }).containsExactlyInAnyOrder(visitorIds[0], visitorIds[1], socialContactWithNoDOB)

    contacts.forEach { contact ->
      assertThat(contact.addresses).hasSize(1)
      assertContactAddress(contact.addresses[0])
    }

    verify(personalRelationshipsApiClient, times(1)).getPrisonerContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithRestrictionId)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithExpiredRestrictionId)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithNoDOB)
  }

  @Test
  fun `when 404 returned from prison API get contacts 404 is returned`() {
    // Given
    val prisonerId = "A1234AA"

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = null,
      approvedVisitorOnly = false,
      httpStatus = HttpStatus.NOT_FOUND,
    )
    val responseSpec = callGetSocialContacts(prisonerId, withAddress = false)
      .expectStatus().isNotFound

    verify(personalRelationshipsApiClient, times(1)).getPrisonerContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
    assertErrorResult(responseSpec, HttpStatus.NOT_FOUND, "Contacts not found for - $prisonerId on personal-relationships-api")
  }

  @Test
  fun `when BAD_REQUEST returned from prison API get contacts BAD_REQUEST is returned`() {
    // Given
    val prisonerId = "A1234AA"

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = null,
      approvedVisitorOnly = false,
      httpStatus = HttpStatus.BAD_REQUEST,
    )

    callGetSocialContacts(prisonerId, withAddress = false)
      .expectStatus().isBadRequest

    verify(personalRelationshipsApiClient, times(1)).getPrisonerContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
  }

  @Test
  fun `when 404 returned from prison API get contact address only contact data is returned`() {
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(socialContactWithRestrictionId, socialContactWithExpiredRestrictionId)
    val prisonerContactIds = listOf(999001L, 999002L)

    val prContacts = createPersonalRelationshipsContactsDto(
      contactIds = visitorIds,
      prisonerContactIds = prisonerContactIds,
      isApproved = false,
    )

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = prContacts,
      approvedVisitorOnly = false,
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
              expiryDate = LocalDate.now().minusDays(1),
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

    prisonApiMockServer.stubGetPersonNotFound(socialContactWithRestrictionId)
    prisonApiMockServer.stubGetPersonNotFound(socialContactWithExpiredRestrictionId)

    val returnResult = callGetSocialContacts(prisonerId)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts).hasSize(2)
    assertThat(contacts.map { it.personId }).containsExactlyInAnyOrder(visitorIds[0], visitorIds[1])

    contacts.forEach { contact ->
      assertThat(contact.addresses).hasSize(0)
    }

    verify(personalRelationshipsApiClient, times(1)).getPrisonerContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(2)).getPersonAddress(any())
  }

  @Test
  fun `when personal relationship API restrictions call fails with BAD_REQUEST then entire call fails`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187525)

    val prisonerContactIds = listOf(999001L)
    val prContacts = createPersonalRelationshipsContactsDto(
      contactIds = visitorIds,
      prisonerContactIds = prisonerContactIds,
      isApproved = false,
    )

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = prContacts,
      approvedVisitorOnly = false,
    )

    personalRelationshipsApiMockServer.stubPrisonerContactRestrictions(
      prisonerContactIds = prisonerContactIds,
      response = null,
      httpStatus = HttpStatus.BAD_REQUEST,
    )

    // When
    val result = webTestClient.get().uri("v2/prisoners/$prisonerId/contacts/social/approved")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // Then
    result.expectStatus().isBadRequest
  }

  private fun assertErrorResult(
    responseSpec: WebTestClient.ResponseSpec,
    httpStatusCode: HttpStatusCode = HttpStatusCode.valueOf(org.apache.http.HttpStatus.SC_BAD_REQUEST),
    errorMessage: String? = null,
  ) {
    responseSpec.expectStatus().isEqualTo(httpStatusCode)
    errorMessage?.let {
      val errorResponse =
        TestObjectMapper.mapper.readValue(responseSpec.expectBody().returnResult().responseBody, ErrorResponse::class.java)
      assertThat(errorResponse.developerMessage).isEqualTo(errorMessage)
    }
  }

  private fun getContactResults(returnResult: WebTestClient.BodyContentSpec): Array<ContactDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<ContactDto>::class.java)

  private fun callGetSocialContacts(
    prisonerId: String,
    hasDateOfBirth: Boolean? = null,
    withAddress: Boolean? = null,
  ): WebTestClient.ResponseSpec {
    val uri = "v2/prisoners/$prisonerId/contacts/social?${getSocialContactsQueryParams(hasDateOfBirth, withAddress)}"
    return webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
  }

  private fun getSocialContactsQueryParams(
    hasDateOfBirth: Boolean? = null,
    withAddress: Boolean? = null,
  ): String {
    val queryParams = ArrayList<String>()

    hasDateOfBirth?.let {
      queryParams.add("hasDateOfBirth=$it")
    }
    withAddress?.let {
      queryParams.add("withAddress=$it")
    }

    return queryParams.joinToString("&")
  }
}
