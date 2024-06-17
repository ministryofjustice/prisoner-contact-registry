package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactsDto
import java.time.LocalDate

@Suppress("ClassName")
class PrisonerGetSocialContactsTest : IntegrationTestBase() {
  @SpyBean
  private lateinit var prisonApiClientSpy: PrisonApiClient

  private final val banEndDate = LocalDate.now().plusDays(10)

  private val expiredBannedRestriction = createBanRestriction(
    startDate = LocalDate.of(2012, 9, 13),
    expiryDate = LocalDate.of(2014, 9, 13),
  )

  private val indefinitelyBannedRestriction = createBanRestriction(
    startDate = LocalDate.of(2012, 9, 13),
    expiryDate = null,
  )

  val banEndAfterEndDateRestriction = createBanRestriction(
    startDate = LocalDate.now().minusDays(7),
    expiryDate = banEndDate.plusDays(1),
  )

  val banEndOnEndDateRestriction = createBanRestriction(
    startDate = LocalDate.now().minusDays(7),
    expiryDate = banEndDate,
  )

  val banEndBeforeEndDateRestriction = createBanRestriction(
    startDate = LocalDate.now().minusDays(1),
    expiryDate = banEndDate.minusDays(1),
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

  private val socialUnapprovedContact = createContact(
    lastName = "BVisitor",
    firstName = "Social",
    dateOfBirth = LocalDate.of(1912, 9, 13),
    personId = 5L,
    approvedVisitor = false,
  )

  fun callGetSocialContacts(
    prisonerId: String,
    personId: Long? = null,
    hasDateOfBirth: Boolean? = null,
    notBannedBeforeDate: LocalDate? = null,
    withAddress: Boolean? = null,
    approvedVisitorsOnly: Boolean? = true,
  ): WebTestClient.ResponseSpec {
    val uri = "/prisoners/$prisonerId/contacts/social?${getContactsQueryParams(personId, hasDateOfBirth, notBannedBeforeDate, withAddress, approvedVisitorsOnly)}"
    return webTestClient.get().uri(uri)
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
      .exchange()
  }

  @Nested
  inner class authentication {
    @Test
    fun `requires authentication`() {
      val prisonerId = "A1234AA"
      webTestClient.get().uri("/prisoners/$prisonerId/contacts/social?approvedVisitorsOnly=false")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      val prisonerId = "A1234AA"
      webTestClient.get().uri("/prisoners/$prisonerId/contacts/social?approvedVisitorsOnly=false")
        .headers(setAuthorisation(roles = listOf("AnyThingWillDo")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Access denied")
    }

    @Test
    fun `requires correct role PRISONER_CONTACT_REGISTRY`() {
      val prisonerId = "A1234AA"
      prisonApiMockServer.stubGetOffenderSocialContacts(prisonerId, ContactsDto(emptyList()))
      webTestClient.get().uri("/prisoners/$prisonerId/contacts/social?approvedVisitorsOnly=false")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_CONTACT_REGISTRY")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @Test
  fun `when prisoner has both social and official contacts only social contacts are returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetOffenderSocialContacts(
      prisonerId,
      contacts = ContactsDto(
        listOf(
          socialContactWithExpiredBannedRestriction,
          socialUnapprovedContact,
          socialContactWithNoDOB,
          officialContact,
        ),
      ),
    )

    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithExpiredBannedRestriction.personId!!)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialUnapprovedContact.personId!!)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithNoDOB.personId!!)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(officialContact.personId!!)

    val returnResult = callGetSocialContacts(prisonerId, approvedVisitorsOnly = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(3)
    val contact1 = contacts[0]
    assertContact(contact1, socialContactWithExpiredBannedRestriction)
    assertContactAddress(contact1.addresses[0])

    val contact2 = contacts[1]
    assertContact(contact2, socialUnapprovedContact)
    assertContactAddress(contact2.addresses[0])

    val contact3 = contacts[2]
    assertContact(contact3, socialContactWithNoDOB)
    assertContactAddress(contact3.addresses[0])

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithExpiredBannedRestriction.personId!!)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialUnapprovedContact.personId!!)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithNoDOB.personId!!)
  }

  @Test
  fun `when prisoner has official contacts only and no social contacts no contacts are returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetOffenderSocialContacts(prisonerId, contacts = ContactsDto(listOf(officialContact)))
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithExpiredBannedRestriction.personId!!)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithCurrentBannedRestriction.personId!!)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(officialContact.personId!!)

    val returnResult = callGetSocialContacts(prisonerId, approvedVisitorsOnly = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts).isEmpty()

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
  }

  @Test
  fun `when person id is passed only one contact is returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetOffenderSocialContacts(prisonerId, contacts = ContactsDto(listOf(socialUnapprovedContact, socialContactWithExpiredBannedRestriction, socialContactWithCurrentBannedRestriction)))

    val returnResult = callGetSocialContacts(prisonerId, personId = socialUnapprovedContact.personId, withAddress = false, approvedVisitorsOnly = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(1)
    assertContact(contacts[0], socialUnapprovedContact)

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
  }

  @Test
  fun `when withAddress is false then no call is made to get address`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetOffenderSocialContacts(
      prisonerId,
      contacts = ContactsDto(
        listOf(
          socialContactWithExpiredBannedRestriction,
          socialUnapprovedContact,
          officialContact,
        ),
      ),
    )

    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithExpiredBannedRestriction.personId!!)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialUnapprovedContact.personId!!)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(officialContact.personId!!)

    val returnResult = callGetSocialContacts(prisonerId, withAddress = false, approvedVisitorsOnly = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(2)
    val contact1 = contacts[0]
    assertContact(contact1, socialContactWithExpiredBannedRestriction)
    assertThat(contact1.addresses).isEmpty()

    val contact2 = contacts[1]
    assertContact(contact2, socialUnapprovedContact)
    assertThat(contact1.addresses).isEmpty()

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
  }

  @Test
  fun `when hasDateOfBirth is passed as true only social contacts with a DOB are returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetOffenderSocialContacts(
      prisonerId,
      contacts = ContactsDto(
        listOf(
          socialContactWithNoDOB,
          socialContactWithExpiredBannedRestriction,
          socialUnapprovedContact,
          officialContact,
        ),
      ),
    )

    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithExpiredBannedRestriction.personId!!)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialUnapprovedContact.personId!!)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(officialContact.personId!!)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithNoDOB.personId!!)

    val returnResult = callGetSocialContacts(prisonerId, hasDateOfBirth = true, approvedVisitorsOnly = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(2)
    val contact1 = contacts[0]
    assertContact(contact1, socialContactWithExpiredBannedRestriction)
    assertContactAddress(contact1.addresses[0])

    val contact2 = contacts[1]
    assertContact(contact2, socialUnapprovedContact)
    assertContactAddress(contact2.addresses[0])

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithExpiredBannedRestriction.personId!!)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialUnapprovedContact.personId!!)
  }

  @Test
  fun `when hasDateOfBirth is passed as false all social contacts with or without a DOB are returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetOffenderSocialContacts(
      prisonerId,
      contacts = ContactsDto(
        listOf(
          socialContactWithNoDOB,
          socialContactWithExpiredBannedRestriction,
          socialUnapprovedContact,
          officialContact,
        ),
      ),
    )

    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithExpiredBannedRestriction.personId!!)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialUnapprovedContact.personId!!)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(officialContact.personId!!)
    prisonApiMockServer.stubGetPersonAddressesFullAddress(socialContactWithNoDOB.personId!!)

    val returnResult = callGetSocialContacts(prisonerId, hasDateOfBirth = false, approvedVisitorsOnly = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(3)
    val contact1 = contacts[0]
    assertContact(contact1, socialContactWithExpiredBannedRestriction)
    assertContactAddress(contact1.addresses[0])

    val contact2 = contacts[1]
    assertContact(contact2, socialUnapprovedContact)
    assertContactAddress(contact2.addresses[0])

    val contact3 = contacts[2]
    assertContact(contact3, socialContactWithNoDOB)
    assertContactAddress(contact3.addresses[0])

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithExpiredBannedRestriction.personId!!)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialUnapprovedContact.personId!!)
    verify(prisonApiClientSpy, times(1)).getPersonAddress(socialContactWithNoDOB.personId!!)
  }

  @Test
  fun `when bannedDate is passed any social contacts banned before that date or with banned end date as null are not returned`() {
    val prisonerId = "A1234AA"

    val socialContactWithBanEndAfterEndDate = createContact(
      lastName = "EBannedVisitor",
      firstName = "Social",
      dateOfBirth = LocalDate.of(1931, 9, 13),
      personId = 5L,
      restrictions = listOf(banEndAfterEndDateRestriction),
    )

    val socialContactWithBanEndOnEndDate = createContact(
      lastName = "FBannedVisitor",
      firstName = "Social",
      dateOfBirth = LocalDate.of(1941, 9, 13),
      personId = 6L,
      restrictions = listOf(banEndOnEndDateRestriction),
    )

    val socialContactWithBanEndBeforeEndDate = createContact(
      lastName = "GBannedVisitor",
      firstName = "Social",
      dateOfBirth = LocalDate.of(1951, 9, 13),
      personId = 7L,
      restrictions = listOf(banEndBeforeEndDateRestriction),
    )

    prisonApiMockServer.stubGetOffenderSocialContacts(
      prisonerId,
      contacts = ContactsDto(
        listOf(
          // returned - no restrictions, unapproved.
          socialUnapprovedContact,
          // returned - expired BANNED restriction
          socialContactWithExpiredBannedRestriction,
          // not returned - social contact with BANNED restriction with expiry date as NULL
          socialContactWithCurrentBannedRestriction,
          // not returned - social contact with BANNED restriction with expiry date as after banned end date
          socialContactWithBanEndAfterEndDate,
          // not returned - social contact with BANNED restriction with expiry date on banned end date
          socialContactWithBanEndOnEndDate,
          // returned
          socialContactWithBanEndBeforeEndDate,
          // not returned - official contact
          officialContact,
        ),
      ),
    )

    val returnResult = callGetSocialContacts(prisonerId, notBannedBeforeDate = banEndDate, withAddress = false, approvedVisitorsOnly = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(3)
    assertContact(contacts[0], socialUnapprovedContact)
    assertContact(contacts[1], socialContactWithExpiredBannedRestriction)
    assertContact(contacts[2], socialContactWithBanEndBeforeEndDate)

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
  }

  @Test
  fun `when bannedDate is passed and contact has multiple bans with one as null most relevant ban is used`() {
    val prisonerId = "A1234AA"

    val socialContactWithMultipleBans = createContact(
      lastName = "GBannedVisitor",
      firstName = "Social",
      dateOfBirth = LocalDate.of(1951, 9, 13),
      personId = 7L,
      restrictions = listOf(banEndBeforeEndDateRestriction, indefinitelyBannedRestriction),
    )

    prisonApiMockServer.stubGetOffenderSocialContacts(
      prisonerId,
      contacts = ContactsDto(
        listOf(
          // not returned
          socialContactWithMultipleBans,
          // not returned - official contact
          officialContact,
        ),
      ),
    )

    val returnResult = callGetSocialContacts(prisonerId, notBannedBeforeDate = banEndDate, withAddress = false, approvedVisitorsOnly = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts).isEmpty()

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
  }

  @Test
  fun `when 404 returned from prison API get contacts 404 is returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetOffenderSocialContacts(prisonerId)

    val responseSpec = callGetSocialContacts(prisonerId, notBannedBeforeDate = banEndDate, withAddress = false, approvedVisitorsOnly = false)
      .expectStatus().isNotFound

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, false)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
    assertErrorResult(responseSpec, HttpStatus.NOT_FOUND, "Contacts not found for - $prisonerId on prison-api")
  }

  @Test
  fun `when BAD_REQUEST returned from prison API get contacts BAD_REQUEST is returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetOffenderSocialContacts(prisonerId, httpStatus = HttpStatus.BAD_REQUEST)

    callGetSocialContacts(prisonerId, notBannedBeforeDate = banEndDate, withAddress = false, approvedVisitorsOnly = false)
      .expectStatus().isBadRequest

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, approvedVisitorsOnly = false)
    verify(prisonApiClientSpy, times(0)).getPersonAddress(any())
  }

  @Test
  fun `when 404 returned from prison API get contact address only contact data is returned`() {
    val prisonerId = "A1234AA"

    prisonApiMockServer.stubGetOffenderSocialContacts(
      prisonerId,
      contacts = ContactsDto(listOf(socialUnapprovedContact)),
    )
    prisonApiMockServer.stubGetPersonNotFound(socialUnapprovedContact.personId!!)

    val returnResult = callGetSocialContacts(prisonerId, approvedVisitorsOnly = false)
      .expectStatus().isOk
      .expectBody()

    val contacts = getContactResults(returnResult)
    assertThat(contacts.size).isEqualTo(1)
    assertThat(contacts[0]).isEqualTo(socialUnapprovedContact)
    assertThat(contacts[0].addresses.size).isEqualTo(0)

    verify(prisonApiClientSpy, times(1)).getOffenderContacts(prisonerId, approvedVisitorsOnly = false)
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

  private fun getContactResults(returnResult: WebTestClient.BodyContentSpec): Array<ContactDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<ContactDto>::class.java)
  }

  private fun getContactsQueryParams(
    personId: Long? = null,
    hasDateOfBirth: Boolean? = null,
    notBannedBeforeDate: LocalDate? = null,
    withAddress: Boolean? = null,
    approvedVisitorsOnly: Boolean? = null,
  ): String {
    val queryParams = ArrayList<String>()
    personId?.let {
      queryParams.add("id=$it")
    }
    hasDateOfBirth?.let {
      queryParams.add("hasDateOfBirth=$it")
    }
    notBannedBeforeDate?.let {
      queryParams.add("notBannedBeforeDate=$it")
    }
    withAddress?.let {
      queryParams.add("withAddress=$it")
    }
    approvedVisitorsOnly?.let {
      queryParams.add("approvedVisitorsOnly=$it")
    }

    return queryParams.joinToString("&")
  }
}
