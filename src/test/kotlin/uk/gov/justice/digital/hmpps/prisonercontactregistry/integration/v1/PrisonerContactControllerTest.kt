package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.v1

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.enum.RestrictionType
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.TestObjectMapper
import java.time.LocalDate

@Suppress("ClassName")
class PrisonerContactControllerTest : IntegrationTestBase() {
  @MockitoSpyBean
  private lateinit var prisonApiClient: PrisonApiClient

  private val expiredBannedRestriction = RestrictionDto(
    restrictionId = 123,
    comment = "Comment Here",
    restrictionType = RestrictionType.BANNED.toString(),
    restrictionTypeDescription = "Banned",
    startDate = LocalDate.of(2012, 9, 13),
    expiryDate = LocalDate.of(2014, 9, 13),
    globalRestriction = false,
  )

  private val contactWithFullDetails = ContactDto(
    lastName = "Ireron",
    middleName = "Danger",
    firstName = "Ehicey",
    dateOfBirth = LocalDate.of(1912, 9, 13),
    contactType = "S",
    contactTypeDescription = "Social",
    relationshipCode = "PROB",
    relationshipDescription = "Probation Officer",
    commentText = "Comment Here",
    emergencyContact = false,
    nextOfKin = false,
    personId = 2187521,
    approvedVisitor = false,
    restrictions = listOf(expiredBannedRestriction),
  )

  private val contactWithMinimumDetails = ContactDto(
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
    restrictions = listOf(expiredBannedRestriction),
  )

  fun callGetContacts(
    prisonerId: String,
    withAddress: Boolean? = null,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri("/prisoners/$prisonerId/contacts?${getContactsQueryParams(withAddress)}")
    .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
    .exchange()

  @Nested
  inner class authentication {
    @Test
    fun `requires authentication`() {
      val prisonerId = "A1234AA"
      webTestClient.get().uri("/prisoners/$prisonerId/contacts")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      val prisonerId = "A1234AA"
      webTestClient.get().uri("/prisoners/$prisonerId/contacts")
        .headers(setAuthorisation(roles = listOf("AnyThingWillDo")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Access denied")
    }

    @Test
    fun `requires correct role PRISONER_CONTACT_REGISTRY`() {
      val prisonerId = "A1234AA"
      prisonApiMockServer.stubGetOffenderContacts(prisonerId, ContactsDto(emptyList()))
      webTestClient.get().uri("/prisoners/$prisonerId/contacts")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @Test
  fun `prisoner has one full contact with address`() {
    val prisonerId = "A1234AA"
    val personId: Long = 2187521

    prisonApiMockServer.stubGetOffenderContacts(prisonerId, contacts = ContactsDto(listOf(contactWithFullDetails)))
    prisonApiMockServer.stubGetPersonAddressesFullAddress(personId, createContactsAddressDto())

    val returnResult = callGetContacts(prisonerId)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    Assertions.assertThat(contacts.size).isEqualTo(1)
    val contact = contacts[0]
    assertContact(contact)
    Assertions.assertThat(contact.addresses.size).isEqualTo(1)
    val contactAddress = contact.addresses[0]
    assertContactAddress(contactAddress)
  }

  @Test
  fun `when get contacts call made with withAddress as true address details are returned for a visitor`() {
    val prisonerId = "A1234AA"
    val personId: Long = 2187521

    prisonApiMockServer.stubGetOffenderContacts(prisonerId, ContactsDto(listOf(contactWithFullDetails)))
    prisonApiMockServer.stubGetPersonAddressesFullAddress(personId, createContactsAddressDto())

    val returnResult = callGetContacts(prisonerId, true)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    Assertions.assertThat(contacts.size).isEqualTo(1)
    val contact = contacts[0]
    assertContact(contact)
    Assertions.assertThat(contact.addresses.size).isEqualTo(1)
    val contactAddress = contact.addresses[0]
    assertContactAddress(contactAddress)

    verify(prisonApiClient, times(1)).getPersonAddress(any())
  }

  @Test
  fun `when get contacts call made with withAddress as false address details are not returned for a visitor`() {
    val prisonerId = "A1234AA"
    val personId: Long = 2187521

    prisonApiMockServer.stubGetOffenderContacts(prisonerId, ContactsDto(listOf(contactWithFullDetails)))
    prisonApiMockServer.stubGetPersonAddressesFullAddress(personId, createContactsAddressDto())

    val returnResult = callGetContacts(prisonerId, withAddress = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    Assertions.assertThat(contacts.size).isEqualTo(1)
    val contact = contacts[0]
    assertContact(contact)
    Assertions.assertThat(contact.addresses.size).isEqualTo(0)

    verify(prisonApiClient, times(0)).getPersonAddress(any())
  }

  @Test
  fun `prisoner has one minimum contact`() {
    val prisonerId = "A1234AA"
    val personId: Long = 2187521

    prisonApiMockServer.stubGetOffenderContacts(prisonerId, ContactsDto(listOf(contactWithMinimumDetails)))
    prisonApiMockServer.stubGetPersonAddressesMinimumAddress(personId)

    val returnResult = callGetContacts(prisonerId, true)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    Assertions.assertThat(contacts.size).isEqualTo(1)
    val contact = contacts[0]
    assertMinimumContact(contact)
    Assertions.assertThat(contact.addresses.size).isEqualTo(1)
  }

  @Test
  fun `prisoner has one minimum address`() {
    val prisonerId = "A1234AA"
    val personId: Long = 2187521

    prisonApiMockServer.stubGetOffenderContacts(prisonerId, ContactsDto(listOf(contactWithFullDetails)))
    prisonApiMockServer.stubGetPersonAddressesMinimumAddress(personId)

    callGetContacts(prisonerId)
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.length()").isEqualTo(1)
      .jsonPath("$[0].addresses.length()").isEqualTo(1)
      .jsonPath("$[0].addresses[0].primary").isEqualTo(true)
      .jsonPath("$[0].addresses[0].noFixedAddress").isEqualTo(false)
      .jsonPath("$[0].addresses[0].phones.length()").isEqualTo(1)
      .jsonPath("$[0].addresses[0].phones[0].number").isEqualTo("504 555 24302")
      .jsonPath("$[0].addresses[0].phones[0].type").isEqualTo("BUS")
      .jsonPath("$[0].addresses[0].addressUsages.length()").isEqualTo(1)
  }

  @Test
  fun `prisoner has one contact with no address`() {
    val prisonerId = "A1234AA"
    val personId: Long = 2187521

    prisonApiMockServer.stubGetOffenderContacts(prisonerId, ContactsDto(listOf(contactWithFullDetails)))
    prisonApiMockServer.stubGetPersonAddressesEmpty(personId)

    webTestClient.get().uri("/prisoners/$prisonerId/contacts")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.length()").isEqualTo(1)
      .jsonPath("$[0].addresses.length()").isEqualTo(0)
  }

  @Test
  fun `prisoner has contacts in expected order of last name and then first name`() {
    // Given
    val prisonerId = "A1234AA"
    val contacts = ContactsDto(
      listOf(
        createContactsDto("a", "z"),
        createContactsDto("a", "a"),
        createContactsDto("c", "b"),
        createContactsDto("z", "a"),
      ),
    )

    prisonApiMockServer.stubGetOffenderContacts(prisonerId, contacts)

    // When
    val response = webTestClient.get().uri("/prisoners/$prisonerId/contacts")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // then
    response.expectStatus().isOk
      .expectBody()
      .jsonPath("$.length()").isEqualTo(4)
      .jsonPath("$[0].firstName").isEqualTo("a")
      .jsonPath("$[0].lastName").isEqualTo("a")
      .jsonPath("$[1].firstName").isEqualTo("z")
      .jsonPath("$[1].lastName").isEqualTo("a")
      .jsonPath("$[2].firstName").isEqualTo("c")
      .jsonPath("$[2].lastName").isEqualTo("b")
      .jsonPath("$[3].firstName").isEqualTo("a")
      .jsonPath("$[3].lastName").isEqualTo("z")
  }

  @Test
  fun `prisoner has no contacts`() {
    val prisonerId = "A1234AA"
    prisonApiMockServer.stubGetOffenderContacts(prisonerId, ContactsDto(emptyList()))
    callGetContacts(prisonerId)
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.length()").isEqualTo(0)
  }

  @Test
  fun `prisoner not found`() {
    val prisonerId = "A1234AA"
    prisonApiMockServer.stubGetOffenderNotFound(prisonerId)
    callGetContacts(prisonerId)
      .expectStatus().isNotFound
  }

  @Test
  fun `person not found`() {
    val prisonerId = "A1234AA"
    val personId: Long = 2187521

    prisonApiMockServer.stubGetOffenderContacts(prisonerId, ContactsDto(listOf(contactWithFullDetails)))
    prisonApiMockServer.stubGetPersonNotFound(personId)

    callGetContacts(prisonerId)
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.length()").isEqualTo(1)
      .jsonPath("$[0].addresses.length()").isEqualTo(0)
  }

  @Test
  fun `bad request`() {
    val prisonerId = "A1234AA"
    webTestClient.get().uri("/prisoners/$prisonerId/contacts?id=ABC")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
      .expectStatus().isBadRequest
  }

  private fun getContactResults(returnResult: WebTestClient.BodyContentSpec): Array<ContactDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<ContactDto>::class.java)

  private fun getContactsQueryParams(
    withAddress: Boolean? = null,
  ): String {
    val queryParams = ArrayList<String>()

    withAddress?.let {
      queryParams.add("withAddress=$it")
    }
    return queryParams.joinToString("&")
  }

  fun createContactsDto(firstName: String, lastName: String, visitorId: Long = 1): ContactDto = ContactDto(
    lastName = lastName,
    middleName = "Danger",
    firstName = firstName,
    dateOfBirth = LocalDate.of(1912, 9, 13),
    contactType = "S",
    contactTypeDescription = "Social",
    relationshipCode = "PROB",
    relationshipDescription = "Probation Officer",
    commentText = "Comment Here",
    emergencyContact = false,
    nextOfKin = false,
    personId = visitorId,
    approvedVisitor = false,
  )
}
