package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.AddressDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.mock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.prisonercontactregistry.service.BANNED_RESTRICTION_TYPE
import java.time.LocalDate

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  companion object {
    internal val prisonApiMockServer = PrisonApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      prisonApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonApiMockServer.stop()
    }
  }

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  @BeforeEach
  fun resetStubs() {
    prisonApiMockServer.resetAll()
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
    assertThat(contact.restrictions[0].restrictionType).isEqualTo(BANNED_RESTRICTION_TYPE)
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
    assertThat(actualContact.approvedVisitor).isTrue()
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
    assertThat(contact.restrictions[0].restrictionType).isEqualTo(BANNED_RESTRICTION_TYPE)
    assertThat(contact.restrictions[0].restrictionTypeDescription).isEqualTo("Banned")
    assertThat(contact.restrictions[0].startDate).isEqualTo("2012-09-13")
    assertThat(contact.restrictions[0].globalRestriction).isEqualTo(false)
  }

  fun assertContactAddress(contactAddress: AddressDto) {
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
  ): ContactDto {
    return ContactDto(
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
      approvedVisitor = true,
      restrictions = restrictions,
    )
  }
  final fun createBanRestriction(
    startDate: LocalDate,
    expiryDate: LocalDate? = null,
    globalRestriction: Boolean = false,
    comment: String = "Comment Here",
  ): RestrictionDto {
    return RestrictionDto(
      restrictionType = BANNED_RESTRICTION_TYPE,
      restrictionTypeDescription = "Banned",
      startDate = startDate,
      expiryDate = expiryDate,
      globalRestriction = globalRestriction,
      comment = comment,
    )
  }
}
