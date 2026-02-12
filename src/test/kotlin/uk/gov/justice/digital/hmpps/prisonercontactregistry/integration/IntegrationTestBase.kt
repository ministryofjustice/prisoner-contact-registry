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
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.GlobalContactRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.mock.PersonalRelationshipsApiMockServer
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
    internal val personalRelationshipsApiMockServer = PersonalRelationshipsApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      personalRelationshipsApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      personalRelationshipsApiMockServer.stop()
    }
  }

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  @BeforeEach
  fun resetStubs() {
    personalRelationshipsApiMockServer.resetAll()
  }

  internal fun setAuthorisation(
    user: String = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

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
}
