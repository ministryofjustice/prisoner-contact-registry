package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonercontactregistry.controller.V2_PRISONER_GET_SOCIAL_CONTACTS_APPROVED_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.IntegrationTestBase
import java.time.LocalDate

@Suppress("ClassName")
@DisplayName("PrisonerContactControllerV2 - $V2_PRISONER_GET_SOCIAL_CONTACTS_APPROVED_CONTROLLER_PATH")
class PrisonerGetApprovedSocialContactsTest : IntegrationTestBase() {
  @SpyBean
  private lateinit var prisonApiClientSpy: PrisonApiClient

  private val expiredBannedRestriction = createBanRestriction(
    startDate = LocalDate.of(2012, 9, 13),
    expiryDate = LocalDate.of(2014, 9, 13),
  )

  private val indefinitelyBannedRestriction = createBanRestriction(
    startDate = LocalDate.of(2012, 9, 13),
    expiryDate = null,
  )

  private val socialContactWithExpiredBannedRestriction = createContact(
    lastName = "BVisitor",
    firstName = "Social",
    dateOfBirth = LocalDate.of(1912, 9, 13),
    personId = 1L,
    restrictions = listOf(expiredBannedRestriction),
  )

  private val socialContactWithCurrentBannedRestriction = createContact(
    lastName = "ABannedVisitor",
    firstName = "Social",
    dateOfBirth = LocalDate.of(1921, 9, 13),
    personId = 2L,
    restrictions = listOf(indefinitelyBannedRestriction),
  )

  private val socialContactWithNoDOB = createContact(
    lastName = "CNoDobVisitor",
    firstName = "Social",
    dateOfBirth = null,
    personId = 3L,
    restrictions = emptyList(),
  )

  private val officialContact = createContact(
    lastName = "Business",
    firstName = "Official",
    contactType = "O",
    dateOfBirth = LocalDate.of(1912, 9, 13),
    personId = 4L,
    restrictions = emptyList(),
  )

  fun callGetApprovedSocialContacts(
    prisonerId: String,
    hasDateOfBirth: Boolean? = null,
    withAddress: Boolean? = null,
  ): WebTestClient.ResponseSpec {
    val uri = "v2/prisoners/$prisonerId/contacts/social/approved?${getContactsQueryParams(hasDateOfBirth, withAddress)}"
    return webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
  }

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
      val prisonerId = "A1234AA"
      prisonApiMockServer.stubGetApprovedOffenderContacts(prisonerId, ContactsDto(emptyList()))
      webTestClient.get().uri("v2/prisoners/$prisonerId/contacts/social/approved")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @Test
  fun `when prisoner has both social and official contacts only social contacts are returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      prisonerId,
      contacts = ContactsDto(
        listOf(
          socialContactWithExpiredBannedRestriction,
          socialContactWithCurrentBannedRestriction,
          socialContactWithNoDOB,
          officialContact,
        ),
      ),
    )

    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithExpiredBannedRestriction.personId!!, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithCurrentBannedRestriction.personId!!, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithNoDOB.personId!!, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(officialContact.personId!!, createContactsAddressDto())

    val returnResult = callGetApprovedSocialContacts(prisonerId)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(3)
    val contact1 = contacts[0]
    assertContact(contact1, socialContactWithCurrentBannedRestriction)
    assertContactAddress(contact1.addresses[0])

    val contact2 = contacts[1]
    assertContact(contact2, socialContactWithExpiredBannedRestriction)
    assertContactAddress(contact2.addresses[0])

    val contact3 = contacts[2]
    assertContact(contact3, socialContactWithNoDOB)
    assertContactAddress(contact3.addresses[0])

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, true)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithExpiredBannedRestriction.personId!!)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithCurrentBannedRestriction.personId!!)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithNoDOB.personId!!)
  }

  @Test
  fun `when prisoner has official contacts only and no social contacts no contacts are returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetApprovedOffenderContacts(prisonerId, contacts = ContactsDto(listOf(officialContact)))
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithExpiredBannedRestriction.personId!!, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithCurrentBannedRestriction.personId!!, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(officialContact.personId!!, createContactsAddressDto())

    val returnResult = callGetApprovedSocialContacts(prisonerId)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts).isEmpty()

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, true)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
  }

  @Test
  fun `when withAddress is false then no call is made to get address`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      prisonerId,
      contacts = ContactsDto(
        listOf(
          socialContactWithExpiredBannedRestriction,
          socialContactWithCurrentBannedRestriction,
          officialContact,
        ),
      ),
    )

    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithExpiredBannedRestriction.personId!!, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithCurrentBannedRestriction.personId!!, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(officialContact.personId!!, createContactsAddressDto())

    val returnResult = callGetApprovedSocialContacts(prisonerId, withAddress = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(2)
    val contact1 = contacts[0]
    assertContact(contact1, socialContactWithCurrentBannedRestriction)
    assertThat(contact1.addresses).isEmpty()

    val contact2 = contacts[1]
    assertContact(contact2, socialContactWithExpiredBannedRestriction)
    assertThat(contact1.addresses).isEmpty()

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, true)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
  }

  @Test
  fun `when hasDateOfBirth is passed as true only social contacts with a DOB are returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      prisonerId,
      contacts = ContactsDto(
        listOf(
          socialContactWithNoDOB,
          socialContactWithExpiredBannedRestriction,
          socialContactWithCurrentBannedRestriction,
          officialContact,
        ),
      ),
    )

    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithExpiredBannedRestriction.personId!!, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithCurrentBannedRestriction.personId!!, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(officialContact.personId!!, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithNoDOB.personId!!, createContactsAddressDto())

    val returnResult = callGetApprovedSocialContacts(prisonerId, hasDateOfBirth = true)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(2)
    val contact1 = contacts[0]
    assertContact(contact1, socialContactWithCurrentBannedRestriction)
    assertContactAddress(contact1.addresses[0])

    val contact2 = contacts[1]
    assertContact(contact2, socialContactWithExpiredBannedRestriction)
    assertContactAddress(contact2.addresses[0])

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, true)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithExpiredBannedRestriction.personId!!)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithCurrentBannedRestriction.personId!!)
  }

  @Test
  fun `when hasDateOfBirth is passed as false all social contacts with or without a DOB are returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      prisonerId,
      contacts = ContactsDto(
        listOf(
          socialContactWithNoDOB,
          socialContactWithExpiredBannedRestriction,
          socialContactWithCurrentBannedRestriction,
          officialContact,
        ),
      ),
    )

    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithExpiredBannedRestriction.personId!!, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithCurrentBannedRestriction.personId!!, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(officialContact.personId!!, createContactsAddressDto())
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithNoDOB.personId!!, createContactsAddressDto())

    val returnResult = callGetApprovedSocialContacts(prisonerId, hasDateOfBirth = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(3)
    val contact1 = contacts[0]
    assertContact(contact1, socialContactWithCurrentBannedRestriction)
    assertContactAddress(contact1.addresses[0])

    val contact2 = contacts[1]
    assertContact(contact2, socialContactWithExpiredBannedRestriction)
    assertContactAddress(contact2.addresses[0])

    val contact3 = contacts[2]
    assertContact(contact3, socialContactWithNoDOB)
    assertContactAddress(contact3.addresses[0])

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, true)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithExpiredBannedRestriction.personId!!)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithCurrentBannedRestriction.personId!!)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithNoDOB.personId!!)
  }

  @Test
  fun `when 404 returned from prison API get contacts 404 is returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetApprovedOffenderContacts(prisonerId, contacts = null)

    val responseSpec = callGetApprovedSocialContacts(prisonerId, withAddress = false)
      .expectStatus().isNotFound

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, true)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
    assertErrorResult(responseSpec, HttpStatus.NOT_FOUND, "Contacts not found for - $prisonerId on prison-api")
  }

  @Test
  fun `when BAD_REQUEST returned from prison API get contacts BAD_REQUEST is returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetApprovedOffenderContacts(prisonerId, contacts = null, HttpStatus.BAD_REQUEST)

    callGetApprovedSocialContacts(prisonerId, withAddress = false)
      .expectStatus().isBadRequest

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, true)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
  }

  @Test
  fun `when 404 returned from prison API get contact address only contact data is returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      prisonerId,
      contacts = ContactsDto(listOf(socialContactWithExpiredBannedRestriction)),
    )
    prisonApiMockServer.stubGetPersonNotFound(socialContactWithExpiredBannedRestriction.personId!!)

    val returnResult = callGetApprovedSocialContacts(prisonerId)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(1)
    assertThat(contacts[0]).isEqualTo(socialContactWithExpiredBannedRestriction)
    assertThat(contacts[0].addresses.size).isEqualTo(0)

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, true)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(any())
  }

  private fun assertErrorResult(
    responseSpec: WebTestClient.ResponseSpec,
    httpStatusCode: HttpStatusCode = HttpStatusCode.valueOf(org.apache.http.HttpStatus.SC_BAD_REQUEST),
    errorMessage: String? = null,
  ) {
    responseSpec.expectStatus().isEqualTo(httpStatusCode)
    errorMessage?.let {
      val errorResponse =
        objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, ErrorResponse::class.java)
      assertThat(errorResponse.developerMessage).isEqualTo(errorMessage)
    }
  }

  @Test
  fun `bad request`() {
    val prisonerId = "A1234AA"
    webTestClient.get().uri("/prisoners/$prisonerId/contacts?id=ABC")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
      .expectStatus().isBadRequest
  }

  private fun getContactResults(returnResult: WebTestClient.BodyContentSpec): Array<ContactDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<ContactDto>::class.java)
  }

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
}
