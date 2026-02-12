package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonercontactregistry.controller.V2_PRISONER_GET_SOCIAL_CONTACTS_APPROVED_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.TestObjectMapper
import java.time.LocalDate

@Suppress("ClassName")
@DisplayName("PrisonerContactControllerV2 - $V2_PRISONER_GET_SOCIAL_CONTACTS_APPROVED_CONTROLLER_PATH")
class PrisonerGetApprovedSocialContactsTest : IntegrationTestBase() {
  @MockitoSpyBean
  private lateinit var personalRelationshipsApiClientSpy: PersonalRelationshipsApiClient

  val socialContactWithRestrictionId = 1L
  val socialContactWithExpiredRestrictionId = 2L
  val socialContactWithNoDOB = 3L

  @Nested
  inner class authentication {
    @Test
    fun `requires authentication`() {
      val prisonerId = "A1234AA"
      webTestClient.get().uri("v2/prisoners/$prisonerId/contacts/social/approved")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      val prisonerId = "A1234AA"
      webTestClient.get().uri("v2/prisoners/$prisonerId/contacts/social/approved")
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
      val visitorIds: List<Long> = listOf(2187525L)

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
      val result = webTestClient.get().uri("v2/prisoners/$prisonerId/contacts/social/approved")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
        .exchange()

      // Then
      result.expectStatus().isOk
    }
  }

  @Test
  fun `when hasDateOfBirth is passed as true only social contacts with a DOB are returned`() {
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(socialContactWithRestrictionId, socialContactWithExpiredRestrictionId)
    val prisonerContactIds = listOf(999001L, 999002L)

    val prContacts = mutableListOf<PersonalRelationshipsContactDto>()

    prContacts.addAll(
      createPersonalRelationshipsContactDtoList(
        contactIds = visitorIds,
        prisonerContactIds = prisonerContactIds,
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
        isApprovedVisitor = true,
        isEmergencyContact = false,
        isNextOfKin = false,
        comments = "Comment Here",
        flat = "Flat 1",
        property = "221B",
        street = "Baker Street",
        area = "Marylebone",
        cityDescription = "London",
        countyDescription = "Greater London",
        postcode = "NW1 6XE",
        countryDescription = "England",
        noFixedAddress = false,
        primaryAddress = true,
      ),
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

    val returnResult = callGetApprovedSocialContacts(prisonerId, hasDateOfBirth = true)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)

    assertThat(contacts).hasSize(2)
    assertThat(contacts.map { it.personId }).containsExactlyInAnyOrder(visitorIds[0], visitorIds[1])

    contacts.forEach { contact ->
      assertThat(contact.addresses).hasSize(1)
      assertContactAddress(contact.addresses[0])
    }

    verify(personalRelationshipsApiClientSpy, times(1)).getPrisonerContacts(prisonerId, true)
  }

  @Test
  fun `when hasDateOfBirth is passed as false all social contacts with or without a DOB are returned`() {
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(socialContactWithRestrictionId, socialContactWithExpiredRestrictionId)
    val prisonerContactIds = listOf(999001L, 999002L)

    val prContacts = mutableListOf<PersonalRelationshipsContactDto>()

    prContacts.addAll(
      createPersonalRelationshipsContactDtoList(
        contactIds = visitorIds,
        prisonerContactIds = prisonerContactIds,
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
        isApprovedVisitor = true,
        isEmergencyContact = false,
        isNextOfKin = false,
        comments = "Comment Here",
        flat = "Flat 1",
        property = "221B",
        street = "Baker Street",
        area = "Marylebone",
        cityDescription = "London",
        countyDescription = "Greater London",
        postcode = "NW1 6XE",
        countryDescription = "England",
        noFixedAddress = false,
        primaryAddress = true,
      ),
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

    val returnResult = callGetApprovedSocialContacts(prisonerId, hasDateOfBirth = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)

    assertThat(contacts).hasSize(3)
    assertThat(contacts.map { it.personId }).containsExactlyInAnyOrder(visitorIds[0], visitorIds[1], socialContactWithNoDOB)

    contacts.forEach { contact ->
      assertThat(contact.addresses).hasSize(1)
      assertContactAddress(contact.addresses[0])
    }

    verify(personalRelationshipsApiClientSpy, times(1)).getPrisonerContacts(prisonerId, true)
  }

  @Test
  fun `when 404 returned from personal relationships API get contacts 404 is returned`() {
    // Given
    val prisonerId = "A1234AA"

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = null,
      approvedVisitorOnly = true,
      httpStatus = HttpStatus.NOT_FOUND,
    )

    val responseSpec = callGetApprovedSocialContacts(prisonerId, withAddress = false).expectStatus().isNotFound

    verify(personalRelationshipsApiClientSpy, times(1)).getPrisonerContacts(prisonerId, true)
    assertErrorResult(responseSpec, HttpStatus.NOT_FOUND, "Contacts not found for - $prisonerId on personal-relationships-api")
  }

  @Test
  fun `when BAD_REQUEST returned from personal relationships API get contacts BAD_REQUEST is returned`() {
    // Given
    val prisonerId = "A1234AA"

    personalRelationshipsApiMockServer.stubGetAllContacts(
      prisonerId = prisonerId,
      contacts = null,
      approvedVisitorOnly = true,
      httpStatus = HttpStatus.BAD_REQUEST,
    )

    callGetApprovedSocialContacts(prisonerId, withAddress = false)
      .expectStatus().isBadRequest

    verify(personalRelationshipsApiClientSpy, times(1)).getPrisonerContacts(prisonerId, true)
  }

  @Test
  fun `when personal relationship API restrictions call fails with BAD_REQUEST then entire call fails`() {
    // Given
    val prisonerId = "A1234AA"
    val visitorIds: List<Long> = listOf(2187525L)

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

  private fun getContactsQueryParams(
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

  private fun callGetApprovedSocialContacts(
    prisonerId: String,
    hasDateOfBirth: Boolean? = null,
    withAddress: Boolean? = null,
  ): WebTestClient.ResponseSpec {
    val uri = "v2/prisoners/$prisonerId/contacts/social/approved?${getContactsQueryParams(hasDateOfBirth, withAddress)}"
    return webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
  }
}
