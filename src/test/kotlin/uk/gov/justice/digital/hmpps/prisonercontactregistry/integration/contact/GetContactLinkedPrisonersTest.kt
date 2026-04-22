package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.controller.GET_CONTACT_LINKED_PRISONERS_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.ContactLinkedPrisonerDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.TestObjectMapper

@Suppress("ClassName")
@DisplayName("ContactsController - Get Contact linked prisoners - $GET_CONTACT_LINKED_PRISONERS_CONTROLLER_PATH")
class GetContactLinkedPrisonersTest : IntegrationTestBase() {
  @Nested
  inner class authentication {
    @Test
    fun `requires authentication`() {
      val contactId = 2187525L

      webTestClient.get().uri(GET_CONTACT_LINKED_PRISONERS_CONTROLLER_PATH.replace("{contactId}", contactId.toString()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      val contactId = 2187525L

      webTestClient.get().uri(GET_CONTACT_LINKED_PRISONERS_CONTROLLER_PATH.replace("{contactId}", contactId.toString()))
        .headers(setAuthorisation(roles = listOf("AnyThingWillDo")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Access denied")
    }
  }

  @Test
  fun `when call to get contact linked prisoners is made then list of prisonerIds is returned`() {
    val contactId = 2187525L
    val linkedPrisonerIds = listOf(ContactLinkedPrisonerDto("123"), ContactLinkedPrisonerDto("456"))

    personalRelationshipsApiMockServer.stubGetContactLinkedPrisoners(contactId, linkedPrisoners = linkedPrisonerIds)

    val returnResult = callGetContactLinkedPrisoners(contactId)
      .expectStatus().isOk
      .expectBody()

    val returnedContactLinkedPrisonersList = getResults(returnResult)
    assertThat(returnedContactLinkedPrisonersList.size).isEqualTo(2)

    verify(personalRelationshipsApiClientSpy, times(1)).getContactLinkedPrisoners(contactId)
  }

  @Test
  fun `when personal relationship API get contact linked prisoners call fails with BAD_REQUEST then entire call fails`() {
    val contactId = 2187525L

    personalRelationshipsApiMockServer.stubGetContactLinkedPrisoners(contactId, linkedPrisoners = null, httpStatus = HttpStatus.BAD_REQUEST)

    callGetContactLinkedPrisoners(contactId)
      .expectStatus().isBadRequest
      .expectBody()
  }

  @Test
  fun `when personal relationship API get contact call fails with NOT_FOUND then entire call fails`() {
    val contactId = 2187525L

    personalRelationshipsApiMockServer.stubGetContactLinkedPrisoners(contactId, linkedPrisoners = null, httpStatus = HttpStatus.NOT_FOUND)

    callGetContactLinkedPrisoners(contactId)
      .expectStatus().isNotFound
      .expectBody()
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<ContactLinkedPrisonerDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<ContactLinkedPrisonerDto>::class.java).toList()

  private fun callGetContactLinkedPrisoners(
    contactId: Long,
  ): WebTestClient.ResponseSpec {
    val uri = GET_CONTACT_LINKED_PRISONERS_CONTROLLER_PATH.replace("{contactId}", contactId.toString())
    return webTestClient
      .get()
      .uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
  }
}
