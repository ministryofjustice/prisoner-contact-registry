package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("ClassName")
class PrisonerContactControllerTest : IntegrationTestBase() {

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

    webTestClient.get().uri("/prisoners/$prisonerId/contacts")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.length()").isEqualTo(1)
      .jsonPath("$[0].personId").isEqualTo("2187521")
      .jsonPath("$[0].firstName").isEqualTo("Ehicey")
      .jsonPath("$[0].middleName").isEqualTo("Danger")
      .jsonPath("$[0].lastName").isEqualTo("Ireron")
      .jsonPath("$[0].dateOfBirth").isEqualTo("1912-09-13")
      .jsonPath("$[0].relationshipCode").isEqualTo("PROB")
      .jsonPath("$[0].relationshipDescription").isEqualTo("Probation Officer")
      .jsonPath("$[0].contactType").isEqualTo("O")
      .jsonPath("$[0].contactTypeDescription").isEqualTo("Official")
      .jsonPath("$[0].approvedVisitor").isEqualTo("false")
      .jsonPath("$[0].emergencyContact").isEqualTo("false")
      .jsonPath("$[0].nextOfKin").isEqualTo("false")
      .jsonPath("$[0].commentText").isEqualTo("Comment Here")
      .jsonPath("$[0].restrictions.length()").isEqualTo(1)
      .jsonPath("$[0].restrictions[0].restrictionType").isEqualTo("BAN")
      .jsonPath("$[0].restrictions[0].restrictionTypeDescription").isEqualTo("Banned")
      .jsonPath("$[0].restrictions[0].startDate").isEqualTo("2012-09-13")
      .jsonPath("$[0].restrictions[0].expiryDate").isEqualTo("2014-09-13")
      .jsonPath("$[0].restrictions[0].globalRestriction").isEqualTo(false)
      .jsonPath("$[0].restrictions[0].comment").isEqualTo("Comment Here")
      .jsonPath("$[0].addresses.length()").isEqualTo(1)
      .jsonPath("$[0].addresses[0].addressType").isEqualTo("BUS")
      .jsonPath("$[0].addresses[0].flat").isEqualTo("3B")
      .jsonPath("$[0].addresses[0].premise").isEqualTo("Liverpool Prison")
      .jsonPath("$[0].addresses[0].street").isEqualTo("Slinn Street")
      .jsonPath("$[0].addresses[0].locality").isEqualTo("Brincliffe")
      .jsonPath("$[0].addresses[0].town").isEqualTo("Birmingham")
      .jsonPath("$[0].addresses[0].postalCode").isEqualTo("D7 5CC")
      .jsonPath("$[0].addresses[0].county").isEqualTo("West Midlands")
      .jsonPath("$[0].addresses[0].country").isEqualTo("England")
      .jsonPath("$[0].addresses[0].primary").isEqualTo(true)
      .jsonPath("$[0].addresses[0].noFixedAddress").isEqualTo(false)
      .jsonPath("$[0].addresses[0].startDate").isEqualTo("2012-05-01")
      .jsonPath("$[0].addresses[0].endDate").isEqualTo("2016-05-01")
      .jsonPath("$[0].addresses[0].phones.length()").isEqualTo(1)
      .jsonPath("$[0].addresses[0].phones[0].number").isEqualTo("504 555 24302")
      .jsonPath("$[0].addresses[0].phones[0].type").isEqualTo("BUS")
      .jsonPath("$[0].addresses[0].phones[0].ext").isEqualTo("123")
      .jsonPath("$[0].addresses[0].addressUsages.length()").isEqualTo(1)
      .jsonPath("$[0].addresses[0].addressUsages[0].addressUsage").isEqualTo("HDC")
      .jsonPath("$[0].addresses[0].addressUsages[0].addressUsageDescription").isEqualTo("HDC Address")
      .jsonPath("$[0].addresses[0].addressUsages[0].activeFlag").isEqualTo(true)
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
}
