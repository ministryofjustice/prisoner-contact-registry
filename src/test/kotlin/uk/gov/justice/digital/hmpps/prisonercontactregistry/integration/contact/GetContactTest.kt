package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.controller.CONTACTS_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.TestObjectMapper

@Suppress("ClassName")
@DisplayName("ContactsController - Get Contact - $CONTACTS_CONTROLLER_PATH")
class GetContactTest : IntegrationTestBase() {
  @Nested
  inner class authentication {
    @Test
    fun `requires authentication`() {
      val contactId = 2187525L

      webTestClient.get().uri(CONTACTS_CONTROLLER_PATH.replace("{contactId}", contactId.toString()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      val contactId = 2187525L

      webTestClient.get().uri(CONTACTS_CONTROLLER_PATH.replace("{contactId}", contactId.toString()))
        .headers(setAuthorisation(roles = listOf("AnyThingWillDo")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Access denied")
    }
  }

  @Test
  fun `when call to get contact is made then contact is returned`() {
    val contactId = 2187525L

    personalRelationshipsApiMockServer.stubGetContact(contactId, createPersonalRelationshipsContactDto(contactId = contactId))

    val returnResult = callGetContact(contactId)
      .expectStatus().isOk
      .expectBody()

    val contact = getResults(returnResult)
    assertThat(contact.contactId).isEqualTo(contactId)

    verify(personalRelationshipsApiClientSpy, times(1)).getContact(contactId)
  }

  @Test
  fun `when personal relationship API get contact call fails with BAD_REQUEST then entire call fails`() {
    val contactId = 2187525L

    personalRelationshipsApiMockServer.stubGetContact(contactId, null, HttpStatus.BAD_REQUEST)

    callGetContact(contactId)
      .expectStatus().isBadRequest
      .expectBody()
  }

  @Test
  fun `when personal relationship API get contact call fails with NOT_FOUND then entire call fails`() {
    val contactId = 2187525L

    personalRelationshipsApiMockServer.stubGetContact(contactId, null, HttpStatus.NOT_FOUND)

    callGetContact(contactId)
      .expectStatus().isNotFound
      .expectBody()
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): ContactDto = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, ContactDto::class.java)

  private fun callGetContact(
    contactId: Long,
  ): WebTestClient.ResponseSpec {
    val uri = CONTACTS_CONTROLLER_PATH.replace("{contactId}", contactId.toString())
    return webTestClient
      .get()
      .uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
  }
}
