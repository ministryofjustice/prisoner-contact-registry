package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.contact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.controller.GET_CONTACT_GLOBAL_RESTRICTIONS_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.TestObjectMapper

@Suppress("ClassName")
@DisplayName("ContactsController - $GET_CONTACT_GLOBAL_RESTRICTIONS_CONTROLLER_PATH")
class GetContactGlobalRestrictionsTest : IntegrationTestBase() {
  @Nested
  inner class authentication {
    @Test
    fun `requires authentication`() {
      val contactId = 2187525L

      webTestClient.get().uri(GET_CONTACT_GLOBAL_RESTRICTIONS_CONTROLLER_PATH.replace("{contactId}", contactId.toString()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      val contactId = 2187525L

      webTestClient.get().uri(GET_CONTACT_GLOBAL_RESTRICTIONS_CONTROLLER_PATH.replace("{contactId}", contactId.toString()))
        .headers(setAuthorisation(roles = listOf("AnyThingWillDo")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Access denied")
    }
  }

  @Test
  fun `when call to get global restrictions is made then all existing global restrictions are returned`() {
    val contactId = 2187525L
    val restriction1 = createGlobalRestriction(contactRestrictionId = 1L, contactId = contactId, restrictionType = "BAN")
    val restriction2 = createGlobalRestriction(contactRestrictionId = 2L, contactId = contactId, restrictionType = "CLOSED")
    val globalRestrictions = listOf(
      restriction1,
      restriction2,
    )

    personalRelationshipsApiMockServer.stubGetContactGlobalRestrictions(contactId, globalRestrictions)

    val returnResult = callGetContactGlobalRestrictions(contactId)
      .expectStatus().isOk
      .expectBody()

    val restrictions = getResults(returnResult)

    assertThat(restrictions.size).isEqualTo(2)
    assertThat(restrictions[0].restrictionId).isEqualTo(restriction1.contactRestrictionId)
    assertThat(restrictions[0].restrictionType).isEqualTo(restriction1.restrictionType)
    assertThat(restrictions[1].restrictionId).isEqualTo(restriction2.contactRestrictionId)
    assertThat(restrictions[1].restrictionType).isEqualTo(restriction2.restrictionType)

    verify(personalRelationshipsApiClientSpy, times(1)).getContactGlobalRestrictions(contactId)
  }

  @Test
  fun `when personal relationship API restrictions call fails with BAD_REQUEST then entire call fails`() {
    val contactId = 2187525L
    val globalRestrictions = null

    personalRelationshipsApiMockServer.stubGetContactGlobalRestrictions(contactId, globalRestrictions, HttpStatus.BAD_REQUEST)

    callGetContactGlobalRestrictions(contactId)
      .expectStatus().isBadRequest
      .expectBody()
  }

  @Test
  fun `when personal relationship API restrictions call fails with NOT_FOUND then entire call fails`() {
    val contactId = 2187525L
    val globalRestrictions = null

    personalRelationshipsApiMockServer.stubGetContactGlobalRestrictions(contactId, globalRestrictions, HttpStatus.NOT_FOUND)

    callGetContactGlobalRestrictions(contactId)
      .expectStatus().isNotFound
      .expectBody()
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<RestrictionDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<RestrictionDto>::class.java).toList()

  private fun callGetContactGlobalRestrictions(
    contactId: Long,
  ): WebTestClient.ResponseSpec {
    val uri = GET_CONTACT_GLOBAL_RESTRICTIONS_CONTROLLER_PATH.replace("{contactId}", contactId.toString())
    return webTestClient
      .get()
      .uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
  }
}
