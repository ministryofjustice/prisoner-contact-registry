package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.DateRangeDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.visit.scheduler.RequestVisitVisitorRestrictionsBodyDto
import java.time.LocalDate

@Suppress("ClassName")
class GetDateRangeVisitorRestrictionsWhichEffectRequestVisitsTest : IntegrationTestBase() {

  @MockitoSpyBean
  private lateinit var prisonApiClient: PrisonApiClient

  @Nested
  inner class authentication {
    @Test
    fun `Get date ranges for visitors restrictions which effect request visits - requires authentication`() {
      // Given
      val requestDto = RequestVisitVisitorRestrictionsBodyDto(
        prisonerId = "A1234AA",
        visitorIds = listOf("2187525"),
        supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
        currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
      )

      // When
      val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf()))

      // Then
      result.expectStatus().isForbidden
    }

    @Test
    fun `Get date ranges for visitors restrictions which effect request visits - requires correct role PRISONER_CONTACT_REGISTRY`() {
      // Given
      val requestDto = RequestVisitVisitorRestrictionsBodyDto(
        prisonerId = "A1234AA",
        visitorIds = listOf("2187525"),
        supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
        currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
      )

      prisonApiMockServer.stubGetApprovedOffenderContacts(
        requestDto.prisonerId,
        contacts = createContactsDto(restrictions = listOf(), requestDto.visitorIds.map { it.toLong() }.toList()),
      )

      // When
      val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

      // Then
      result.expectStatus().isOk
    }
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - bad request`() {
    // Given
    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = "AA12345",
      visitorIds = listOf(),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().is5xxServerError
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - visitorId not found within list of prisoner contacts`() {
    // Given
    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = "A1234AA",
      visitorIds = listOf("2187524", "2187525"),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    val contactWithMinimumDetails = ContactDto(
      lastName = "Ireron",
      firstName = "Ehicey",
      contactType = "S",
      relationshipCode = "PROB",
      relationshipDescription = "Probation Officer",
      commentText = "Comment Here",
      emergencyContact = false,
      nextOfKin = false,
      personId = 2187524,
      approvedVisitor = false,
      restrictions = listOf(),
    )

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      requestDto.prisonerId,
      contacts = ContactsDto(listOf(contactWithMinimumDetails)),
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().isNotFound
    result.expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("One of the visitors provided could not found")
      .jsonPath("$.developerMessage")
      .isEqualTo("Not all visitors provided (${requestDto.visitorIds}) are contacts for prisoner ${requestDto.prisonerId}")
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - No restrictions found for visitors`() {
    // Given
    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = "A1234AA",
      visitorIds = listOf("2187524", "2187525"),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      requestDto.prisonerId,
      contacts = createContactsDto(listOf(), requestDto.visitorIds.map { it.toLong() }.toList()),
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().isOk
    val foundDateRanges = getResults(result)
    assert(foundDateRanges.isEmpty())
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - Restriction with no expiry date found for visitors, later start date`() {
    // Given
    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = "A1234AA",
      visitorIds = listOf("2187524", "2187525"),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    val restrictions = listOf(
      RestrictionDto(
        restrictionId = 123,
        comment = "Comment Here",
        restrictionType = "CHILD",
        restrictionTypeDescription = "Banned",
        startDate = LocalDate.now().plusDays(5),
        expiryDate = null,
        globalRestriction = false,
      ),
      RestrictionDto(
        restrictionId = 345,
        comment = "Comment Here",
        restrictionType = "OTHER",
        restrictionTypeDescription = "Banned",
        startDate = LocalDate.now(),
        expiryDate = LocalDate.now().plusDays(1),
        globalRestriction = false,
      ),
    )

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      requestDto.prisonerId,
      contacts = createContactsDto(restrictions, requestDto.visitorIds.map { it.toLong() }.toList()),
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().isOk
    val foundDateRanges = getResults(result)
    val expected = listOf(DateRangeDto(fromDate = LocalDate.now().plusDays(5), toDate = LocalDate.now().plusDays(28)))
    assertThat(foundDateRanges).isEqualTo(expected)
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - Restriction with no expiry date found for visitors, earlier start date`() {
    // Given
    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = "A1234AA",
      visitorIds = listOf("2187524", "2187525"),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    val restrictions = listOf(
      RestrictionDto(
        restrictionId = 123,
        comment = "Comment Here",
        restrictionType = "CHILD",
        restrictionTypeDescription = "Banned",
        startDate = LocalDate.now().minusDays(1),
        expiryDate = null,
        globalRestriction = false,
      ),
      RestrictionDto(
        restrictionId = 345,
        comment = "Comment Here",
        restrictionType = "OTHER",
        restrictionTypeDescription = "Banned",
        startDate = LocalDate.now(),
        expiryDate = LocalDate.now().plusDays(1),
        globalRestriction = false,
      ),
    )

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      requestDto.prisonerId,
      contacts = createContactsDto(restrictions, requestDto.visitorIds.map { it.toLong() }.toList()),
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().isOk
    val foundDateRanges = getResults(result)
    val expected = listOf(DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)))

    assertThat(foundDateRanges).isEqualTo(expected)
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - Restriction found and date ranges returned`() {
    // Given
    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = "A1234AA",
      visitorIds = listOf("2187524", "2187525"),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    val restrictions = listOf(
      RestrictionDto(
        restrictionId = 123,
        comment = "Comment Here",
        restrictionType = "CHILD",
        restrictionTypeDescription = "Banned",
        startDate = LocalDate.now().plusDays(5),
        expiryDate = LocalDate.now().plusDays(10),
        globalRestriction = false,
      ),
      RestrictionDto(
        restrictionId = 345,
        comment = "Comment Here",
        restrictionType = "CHILD",
        restrictionTypeDescription = "Banned",
        startDate = LocalDate.now(),
        expiryDate = LocalDate.now().plusDays(3),
        globalRestriction = false,
      ),
    )

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      requestDto.prisonerId,
      contacts = createContactsDto(restrictions, requestDto.visitorIds.map { it.toLong() }.toList()),
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().isOk
    val foundDateRanges = getResults(result)
    val expected = listOf(
      DateRangeDto(fromDate = LocalDate.now(), toDate = LocalDate.now().plusDays(3)),
      DateRangeDto(fromDate = LocalDate.now().plusDays(5), toDate = LocalDate.now().plusDays(10)),
    )

    assertThat(foundDateRanges).containsExactlyInAnyOrderElementsOf(expected)
  }

  @Test
  fun `Get date ranges for visitors restrictions which effect request visits - Unsupported restrictions filtered`() {
    // Given
    val requestDto = RequestVisitVisitorRestrictionsBodyDto(
      prisonerId = "A1234AA",
      visitorIds = listOf("2187524", "2187525"),
      supportedVisitorRestrictionsCodesForRequestVisits = listOf("CHILD"),
      currentDateRange = DateRangeDto(fromDate = LocalDate.now().plusDays(2), toDate = LocalDate.now().plusDays(28)),
    )

    val restrictions = listOf(
      RestrictionDto(
        restrictionId = 123,
        comment = "Comment Here",
        restrictionType = "CHILD",
        restrictionTypeDescription = "Banned",
        startDate = LocalDate.now().plusDays(5),
        expiryDate = LocalDate.now().plusDays(10),
        globalRestriction = false,
      ),
      RestrictionDto(
        restrictionId = 345,
        comment = "Comment Here",
        restrictionType = "UNSUPPORTED",
        restrictionTypeDescription = "Banned",
        startDate = LocalDate.now(),
        expiryDate = LocalDate.now().plusDays(3),
        globalRestriction = false,
      ),
    )

    prisonApiMockServer.stubGetApprovedOffenderContacts(
      requestDto.prisonerId,
      contacts = createContactsDto(restrictions, requestDto.visitorIds.map { it.toLong() }.toList()),
    )

    // When
    val result = callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(webTestClient, requestDto, setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))

    // Then
    result.expectStatus().isOk
    val foundDateRanges = getResults(result)
    val expected = listOf(
      DateRangeDto(fromDate = LocalDate.now().plusDays(5), toDate = LocalDate.now().plusDays(10)),
    )

    assertThat(foundDateRanges).containsExactlyInAnyOrderElementsOf(expected)
  }

  private fun callDateRangeVisitorRestrictionsWhichEffectRequestVisitsUri(
    webTestClient: WebTestClient,
    requestDto: RequestVisitVisitorRestrictionsBodyDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val uri = "v2/prisoners/${requestDto.prisonerId}/contacts/social/approved/restrictions/visit-request/date-ranges"

    return webTestClient.post().uri(uri)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(requestDto))
      .exchange()
  }

  private fun getResults(responseSpec: WebTestClient.ResponseSpec): List<DateRangeDto> = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody!!, object : TypeReference<List<DateRangeDto>>() {})
}
