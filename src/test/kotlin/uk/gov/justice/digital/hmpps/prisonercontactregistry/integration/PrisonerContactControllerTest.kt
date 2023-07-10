package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.AddressDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto

@Suppress("ClassName")
class PrisonerContactControllerTest : IntegrationTestBase() {
  @SpyBean
  private lateinit var prisonApiClient: PrisonApiClient

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
      prisonApiMockServer.stubGetOffenderContactsEmpty(prisonerId)
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

    prisonApiMockServer.stubGetOffenderContactFullContact(prisonerId)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(personId)

    val returnResult = webTestClient.get().uri("/prisoners/$prisonerId/contacts")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(1)
    val contact = contacts[0]
    assertContact(contact)
    assertThat(contact.addresses.size).isEqualTo(1)
    val contactAddress = contact.addresses[0]
    assertContactAddress(contactAddress)
  }

  @Test
  fun `when get contacts call made with withAddress as true address details are returned for a visitor`() {
    val prisonerId = "A1234AA"
    val personId: Long = 2187521

    prisonApiMockServer.stubGetOffenderContactFullContact(prisonerId)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(personId)

    val returnResult = webTestClient.get().uri("/prisoners/$prisonerId/contacts?withAddress=true")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(1)
    val contact = contacts[0]
    assertContact(contact)
    assertThat(contact.addresses.size).isEqualTo(1)
    val contactAddress = contact.addresses[0]
    assertContactAddress(contactAddress)

    verify(prisonApiClient, times(1)).getPersonAddress(any())
  }

  @Test
  fun `when get contacts call made with withAddress as true address details are not returned for a visitor`() {
    val prisonerId = "A1234AA"
    val personId: Long = 2187521

    prisonApiMockServer.stubGetOffenderContactFullContact(prisonerId)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(personId)

    val returnResult = webTestClient.get().uri("/prisoners/$prisonerId/contacts?withAddress=false")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(1)
    val contact = contacts[0]
    assertContact(contact)
    assertThat(contact.addresses.size).isEqualTo(0)

    verify(prisonApiClient, times(0)).getPersonAddress(any())
  }

  @Test
  fun `prisoner has one minimum contact`() {
    val prisonerId = "A1234AA"
    val personId: Long = 2187521

    prisonApiMockServer.stubGetOffenderContactMinimumContact(prisonerId)
    prisonApiMockServer.stubGetPersonAddressesMinimumAddress(personId)

    webTestClient.get().uri("/prisoners/$prisonerId/contacts")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.length()").isEqualTo(1)
      .jsonPath("$[0].firstName").isEqualTo("Ehicey")
      .jsonPath("$[0].lastName").isEqualTo("Ireron")
      .jsonPath("$[0].relationshipCode").isEqualTo("PROB")
      .jsonPath("$[0].contactType").isEqualTo("O")
      .jsonPath("$[0].approvedVisitor").isEqualTo("false")
      .jsonPath("$[0].emergencyContact").isEqualTo("false")
      .jsonPath("$[0].nextOfKin").isEqualTo("false")
      .jsonPath("$[0].restrictions.length()").isEqualTo(1)
      .jsonPath("$[0].restrictions[0].restrictionType").isEqualTo("BAN")
      .jsonPath("$[0].restrictions[0].restrictionTypeDescription").isEqualTo("Banned")
      .jsonPath("$[0].restrictions[0].startDate").isEqualTo("2012-09-13")
      .jsonPath("$[0].restrictions[0].globalRestriction").isEqualTo(false)
      .jsonPath("$[0].addresses.length()").isEqualTo(0)
  }

  @Test
  fun `prisoner has one minimum address`() {
    val prisonerId = "A1234AA"
    val personId: Long = 2187521

    prisonApiMockServer.stubGetOffenderContactFullContact(prisonerId)
    prisonApiMockServer.stubGetPersonAddressesMinimumAddress(personId)

    webTestClient.get().uri("/prisoners/$prisonerId/contacts")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
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

    prisonApiMockServer.stubGetOffenderContactFullContact(prisonerId)
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
  fun `prisoner has contacts in expected order`() {
    // Given
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetOffenderContactsForOrderingByNames(prisonerId)

    // When
    val response = webTestClient.get().uri("/prisoners/$prisonerId/contacts")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()

    // then
    response.expectStatus().isOk
      .expectBody()
      .jsonPath("$.length()").isEqualTo(7)
      .jsonPath("$[0].lastName").isEqualTo("Aled")
      .jsonPath("$[0].firstName").isEqualTo("Aeron")
      .jsonPath("$[1].lastName").isEqualTo("Aled")
      .jsonPath("$[1].firstName").isEqualTo("Cynog")
      .jsonPath("$[2].lastName").isEqualTo("Aled")
      .jsonPath("$[2].firstName").isEqualTo("Wyn")
      .jsonPath("$[3].lastName").isEqualTo("Gwyn")
      .jsonPath("$[3].firstName").isEqualTo("Aeron")
      .jsonPath("$[4].lastName").isEqualTo("Gwyn")
      .jsonPath("$[4].firstName").isEqualTo("Cynog")
      .jsonPath("$[5].lastName").isEqualTo("Gwyn")
      .jsonPath("$[5].firstName").isEqualTo("Wyn")
      .jsonPath("$[6].lastName").isEqualTo("Llywelyn")
      .jsonPath("$[6].firstName").isEqualTo("Gruffydd")
  }

  @Test
  fun `prisoner has no contacts`() {
    val prisonerId = "A1234AA"
    prisonApiMockServer.stubGetOffenderContactsEmpty(prisonerId)
    webTestClient.get().uri("/prisoners/$prisonerId/contacts")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.length()").isEqualTo(0)
  }

  @Test
  fun `prisoner not found`() {
    val prisonerId = "A1234AA"
    prisonApiMockServer.stubGetOffenderNotFound(prisonerId)
    webTestClient.get().uri("/prisoners/$prisonerId/contacts")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `person not found`() {
    val prisonerId = "A1234AA"
    val personId: Long = 2187521

    prisonApiMockServer.stubGetOffenderContactFullContact(prisonerId)
    prisonApiMockServer.stubGetPersonNotFound(personId)

    webTestClient.get().uri("/prisoners/$prisonerId/contacts")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
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

  private fun getContactResults(returnResult: WebTestClient.BodyContentSpec): Array<ContactDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<ContactDto>::class.java)
  }

  private fun assertContact(contact: ContactDto) {
    assertThat(contact.personId).isEqualTo(2187521)
    assertThat(contact.firstName).isEqualTo("Ehicey")
    assertThat(contact.middleName).isEqualTo("Danger")
    assertThat(contact.lastName).isEqualTo("Ireron")
    assertThat(contact.dateOfBirth).isEqualTo("1912-09-13")
    assertThat(contact.relationshipCode).isEqualTo("PROB")
    assertThat(contact.relationshipDescription).isEqualTo("Probation Officer")
    assertThat(contact.contactType).isEqualTo("O")
    assertThat(contact.contactTypeDescription).isEqualTo("Official")
    assertThat(contact.approvedVisitor).isFalse
    assertThat(contact.emergencyContact).isFalse
    assertThat(contact.nextOfKin).isFalse
    assertThat(contact.commentText).isEqualTo("Comment Here")
    assertThat(contact.restrictions.size).isEqualTo(1)
    assertThat(contact.restrictions[0].restrictionType).isEqualTo("BAN")
    assertThat(contact.restrictions[0].restrictionTypeDescription).isEqualTo("Banned")
    assertThat(contact.restrictions[0].startDate).isEqualTo("2012-09-13")
    assertThat(contact.restrictions[0].expiryDate).isEqualTo("2014-09-13")
    assertThat(contact.restrictions[0].globalRestriction).isEqualTo(false)
    assertThat(contact.restrictions[0].comment).isEqualTo("Comment Here")
  }

  private fun assertContactAddress(contactAddress: AddressDto) {
    assertThat(contactAddress.addressType).isEqualTo("BUS")
    assertThat(contactAddress.flat).isEqualTo("3B")
    assertThat(contactAddress.premise).isEqualTo("Liverpool Prison")
    assertThat(contactAddress.street).isEqualTo("Slinn Street")
    assertThat(contactAddress.locality).isEqualTo("Brincliffe")
    assertThat(contactAddress.town).isEqualTo("Birmingham")
    assertThat(contactAddress.postalCode).isEqualTo("D7 5CC")
    assertThat(contactAddress.county).isEqualTo("West Midlands")
    assertThat(contactAddress.country).isEqualTo("England")
    assertThat(contactAddress.primary).isEqualTo(true)
    assertThat(contactAddress.noFixedAddress).isEqualTo(false)
    assertThat(contactAddress.startDate).isEqualTo("2012-05-01")
    assertThat(contactAddress.endDate).isEqualTo("2016-05-01")
    assertThat(contactAddress.phones.size).isEqualTo(1)
    assertThat(contactAddress.phones[0].number).isEqualTo("504 555 24302")
    assertThat(contactAddress.phones[0].type).isEqualTo("BUS")
    assertThat(contactAddress.phones[0].ext).isEqualTo("123")
    assertThat(contactAddress.addressUsages.size).isEqualTo(1)
    assertThat(contactAddress.addressUsages[0].addressUsage).isEqualTo("HDC")
    assertThat(contactAddress.addressUsages[0].addressUsageDescription).isEqualTo("HDC Address")
    assertThat(contactAddress.addressUsages[0].activeFlag).isEqualTo(true)
  }
}
