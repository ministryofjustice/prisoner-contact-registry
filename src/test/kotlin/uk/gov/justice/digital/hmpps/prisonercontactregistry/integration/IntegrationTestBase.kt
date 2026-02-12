package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.AddressDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.GlobalContactRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.enum.RestrictionType
import uk.gov.justice.digital.hmpps.prisonercontactregistry.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.mock.PersonalRelationshipsApiMockServer
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.mock.PrisonApiMockServer
import java.time.LocalDate

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(HmppsAuthExtension::class)
@AutoConfigureWebTestClient
abstract class IntegrationTestBase {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  companion object {
    internal val prisonApiMockServer = PrisonApiMockServer()
    internal val personalRelationshipsApiMockServer = PersonalRelationshipsApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      prisonApiMockServer.start()
      personalRelationshipsApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonApiMockServer.stop()
      personalRelationshipsApiMockServer.stop()
    }
  }

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  @BeforeEach
  fun resetStubs() {
    prisonApiMockServer.resetAll()
    personalRelationshipsApiMockServer.resetAll()
  }

  internal fun setAuthorisation(
    user: String = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  fun assertContact(contact: ContactDto) {
    assertThat(contact.personId).isEqualTo(2187521)
    assertThat(contact.firstName).isEqualTo("Ehicey")
    assertThat(contact.middleName).isEqualTo("Danger")
    assertThat(contact.lastName).isEqualTo("Ireron")
    assertThat(contact.dateOfBirth).isEqualTo("1912-09-13")
    assertThat(contact.relationshipCode).isEqualTo("PROB")
    assertThat(contact.relationshipDescription).isEqualTo("Probation Officer")
    assertThat(contact.contactType).isEqualTo("S")
    assertThat(contact.contactTypeDescription).isEqualTo("Social")
    assertThat(contact.approvedVisitor).isFalse()
    assertThat(contact.emergencyContact).isFalse
    assertThat(contact.nextOfKin).isFalse
    assertThat(contact.commentText).isEqualTo("Comment Here")
    assertThat(contact.restrictions.size).isEqualTo(1)
    assertThat(contact.restrictions[0].restrictionType).isEqualTo(RestrictionType.BANNED.toString())
    assertThat(contact.restrictions[0].restrictionTypeDescription).isEqualTo("Banned")
    assertThat(contact.restrictions[0].startDate).isEqualTo("2012-09-13")
    assertThat(contact.restrictions[0].expiryDate).isEqualTo("2014-09-13")
    assertThat(contact.restrictions[0].globalRestriction).isEqualTo(false)
    assertThat(contact.restrictions[0].comment).isEqualTo("Comment Here")
  }

  fun assertContact(actualContact: ContactDto, expectedContact: ContactDto) {
    assertThat(actualContact.personId).isEqualTo(expectedContact.personId)
    assertThat(actualContact.firstName).isEqualTo(expectedContact.firstName)
    assertThat(actualContact.middleName).isEqualTo(expectedContact.middleName)
    assertThat(actualContact.lastName).isEqualTo(expectedContact.lastName)
    assertThat(actualContact.dateOfBirth).isEqualTo(expectedContact.dateOfBirth)
    assertThat(actualContact.relationshipCode).isEqualTo(expectedContact.relationshipCode)
    assertThat(actualContact.relationshipDescription).isEqualTo(expectedContact.relationshipDescription)
    assertThat(actualContact.contactType).isEqualTo("S")
    assertThat(actualContact.contactTypeDescription).isEqualTo("Social")
    assertThat(actualContact.approvedVisitor).isEqualTo(expectedContact.approvedVisitor)
    assertThat(actualContact.emergencyContact).isEqualTo(expectedContact.emergencyContact)
    assertThat(actualContact.nextOfKin).isEqualTo(expectedContact.nextOfKin)
    assertThat(actualContact.commentText).isEqualTo(expectedContact.commentText)
    assertThat(actualContact.restrictions).isEqualTo(expectedContact.restrictions)
  }

  fun assertMinimumContact(contact: ContactDto) {
    assertThat(contact.firstName).isEqualTo("Ehicey")
    assertThat(contact.lastName).isEqualTo("Ireron")
    assertThat(contact.relationshipCode).isEqualTo("PROB")
    assertThat(contact.contactType).isEqualTo("S")
    assertThat(contact.approvedVisitor).isFalse()
    assertThat(contact.emergencyContact).isFalse
    assertThat(contact.nextOfKin).isFalse
    assertThat(contact.restrictions.size).isEqualTo(1)
    assertThat(contact.restrictions[0].restrictionType).isEqualTo(RestrictionType.BANNED.toString())
    assertThat(contact.restrictions[0].restrictionTypeDescription).isEqualTo("Banned")
    assertThat(contact.restrictions[0].startDate).isEqualTo("2012-09-13")
    assertThat(contact.restrictions[0].globalRestriction).isEqualTo(false)
  }

  fun assertContactAddress(contactAddress: AddressDto) {
    assertThat(contactAddress.flat).isEqualTo("Flat 1")
    assertThat(contactAddress.premise).isEqualTo("221B")
    assertThat(contactAddress.street).isEqualTo("Baker Street")
    assertThat(contactAddress.locality).isEqualTo("Marylebone")
    assertThat(contactAddress.town).isEqualTo("London")
    assertThat(contactAddress.postalCode).isEqualTo("NW1 6XE")
    assertThat(contactAddress.county).isEqualTo("Greater London")
    assertThat(contactAddress.country).isEqualTo("England")
    assertThat(contactAddress.primary).isTrue()
    assertThat(contactAddress.noFixedAddress).isFalse()
  }

  final fun createContact(
    lastName: String,
    middleName: String? = null,
    firstName: String,
    dateOfBirth: LocalDate?,
    contactType: String = "S",
    contactTypeDescription: String? = "Social",
    relationshipCode: String = "PROB",
    relationshipDescription: String? = "Probation Officer",
    commentText: String? = "Comment Here",
    emergencyContact: Boolean = false,
    nextOfKin: Boolean = false,
    personId: Long,
    restrictions: List<RestrictionDto> = emptyList(),
    approvedVisitor: Boolean = true,
  ): ContactDto = ContactDto(
    lastName = lastName,
    middleName = middleName,
    firstName = firstName,
    dateOfBirth = dateOfBirth,
    contactType = contactType,
    contactTypeDescription = contactTypeDescription,
    relationshipCode = relationshipCode,
    relationshipDescription = relationshipDescription,
    commentText = commentText,
    emergencyContact = emergencyContact,
    nextOfKin = nextOfKin,
    personId = personId,
    approvedVisitor = approvedVisitor,
    restrictions = restrictions,
  )
  final fun createBanRestriction(
    startDate: LocalDate,
    expiryDate: LocalDate? = null,
    globalRestriction: Boolean = false,
    comment: String = "Comment Here",
  ): RestrictionDto = RestrictionDto(
    restrictionId = 123,
    restrictionType = RestrictionType.BANNED.toString(),
    restrictionTypeDescription = "Banned",
    startDate = startDate,
    expiryDate = expiryDate,
    globalRestriction = globalRestriction,
    comment = comment,
  )

  fun createContactsDto(restrictions: List<RestrictionDto> = listOf(), visitorsId: List<Long>): ContactsDto {
    val contacts = visitorsId.mapIndexed { index: Int, visitorId: Long ->
      ContactDto(
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
        personId = visitorId,
        approvedVisitor = false,
        restrictions = if (restrictions.isNotEmpty()) listOf(restrictions[index]) else listOf(),
      )
    }

    return ContactsDto(contacts)
  }

  fun createPersonalRelationshipsContactDtoList(
    contactIds: List<Long>,
    prisonerContactIds: List<Long>,
    isApproved: Boolean = true,
  ): List<PersonalRelationshipsContactDto> {
    require(contactIds.size == prisonerContactIds.size) {
      "contactIds and prisonerContactIds must be the same size"
    }

    return contactIds.mapIndexed { index, contactId ->
      PersonalRelationshipsContactDto(
        contactId = contactId,
        prisonerContactId = prisonerContactIds[index],
        firstName = "test",
        middleNames = "middle",
        lastName = "user",
        dateOfBirth = LocalDate.of(1912, 9, 13),
        relationshipToPrisonerCode = "FRI",
        relationshipToPrisonerDescription = "Friend",
        relationshipTypeCode = "S",
        relationshipTypeDescription = "Social",
        isApprovedVisitor = isApproved,
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
      )
    }
  }

  fun createLocalRestriction(
    prisonerContactRestrictionId: Long = 1L,
    prisonerContactId: Long,
    contactId: Long,
    prisonerNumber: String,
    restrictionType: String = "CLOSED",
    restrictionTypeDescription: String = restrictionType,
    startDate: LocalDate = LocalDate.of(2024, 1, 1),
    expiryDate: LocalDate? = null,
    comments: String? = "Comment",
    enteredByDisplayName: String = "Test User",
  ): PrisonerContactRestrictionDto = PrisonerContactRestrictionDto(
    prisonerContactRestrictionId = prisonerContactRestrictionId,
    prisonerContactId = prisonerContactId,
    contactId = contactId,
    prisonerNumber = prisonerNumber,
    restrictionType = restrictionType,
    restrictionTypeDescription = restrictionTypeDescription,
    startDate = startDate,
    expiryDate = expiryDate,
    comments = comments,
    enteredByDisplayName = enteredByDisplayName,
  )

  fun createGlobalRestriction(
    contactRestrictionId: Long = 1L,
    contactId: Long,
    restrictionType: String = "ANY",
    restrictionTypeDescription: String = restrictionType,
    startDate: LocalDate = LocalDate.of(2024, 1, 1),
    expiryDate: LocalDate? = null,
    comments: String? = "Comment",
    enteredByDisplayName: String = "Test User",
  ): GlobalContactRestrictionDto = GlobalContactRestrictionDto(
    contactRestrictionId = contactRestrictionId,
    contactId = contactId,
    restrictionType = restrictionType,
    restrictionTypeDescription = restrictionTypeDescription,
    startDate = startDate,
    expiryDate = expiryDate,
    comments = comments,
    enteredByDisplayName = enteredByDisplayName,
  )

  fun createContactsAddressDto(): List<AddressDto> = listOf(
    AddressDto(
      flat = "Flat 1",
      premise = "221B",
      street = "Baker Street",
      locality = "Marylebone",
      town = "London",
      postalCode = "NW1 6XE",
      county = "Greater London",
      country = "England",
      comment = "Comment Here",
      primary = true,
      noFixedAddress = false,
    ),
  )
}
