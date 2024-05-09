package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.AddressDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.AddressUsageDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.TelephoneDto
import java.time.LocalDate

internal class PrisonerContactRegistryServiceTest {

  private lateinit var contactService: PrisonerContactRegistryService

  private val apiClient = mock<PrisonApiClient>()

  private val personTelephone = TelephoneDto(
    number = "0114 2345678",
    type = "TEL",
    ext = "123",
  )
  private val personAddressUsage = AddressUsageDto(
    addressUsage = "HDC",
    addressUsageDescription = "HDC Address",
    activeFlag = false,
  )
  private val personAddress = AddressDto(
    addressType = "BUS",
    flat = "3B",
    premise = "Liverpool Prison",
    street = "Slinn Street",
    locality = "Brincliffe",
    town = "Liverpool",
    postalCode = "LI1 5TH",
    county = "HEREFORD",
    country = "ENG",
    comment = "This is a comment text",
    primary = false,
    noFixedAddress = false,
    startDate = LocalDate.now(),
    endDate = LocalDate.now(),
    phones = listOf(personTelephone),
    addressUsages = listOf(personAddressUsage),
  )
  private val contactRestriction = RestrictionDto(
    restrictionType = "123",
    restrictionTypeDescription = "123",
    startDate = LocalDate.now(),
    expiryDate = LocalDate.now(),
    globalRestriction = false,
    comment = "This is a comment text",
  )
  private val offenderContact = ContactDto(
    personId = 5871791, firstName = "John", middleName = "Mark", lastName = "Smith", dateOfBirth = LocalDate.now(),
    relationshipCode = "RO", relationshipDescription = "Responsible Officer", contactType = "O",
    contactTypeDescription = "Official", approvedVisitor = false, emergencyContact = false, nextOfKin = false,
    restrictions = listOf(contactRestriction), addresses = listOf(personAddress), commentText = "This is a comment text",
  )

  @BeforeEach
  fun setUp() {
    contactService = PrisonerContactRegistryService(apiClient)
  }

  @Test
  fun `Get Contact Returns Contact List`() {
    val prisonerId = "A1234AA"

    whenever(
      apiClient.getOffenderContacts(prisonerId, false),
    ).thenReturn(ContactsDto(listOf(offenderContact)))

    val contacts = contactService.getContactList(prisonerId)

    assertThat(contacts).isNotNull
    assertThat(contacts).isNotEmpty

    verify(apiClient, times(1)).getOffenderContacts(prisonerId, false)
  }

  @Test
  fun `Get Contact Returns Empty Contact List`() {
    val prisonerId = "A1234AA"

    whenever(
      apiClient.getOffenderContacts(prisonerId, false),
    ).thenReturn(ContactsDto(emptyList()))

    val contacts = contactService.getContactList(prisonerId)

    assertThat(contacts).isNotNull
    assertThat(contacts).isEmpty()

    verify(apiClient, times(1)).getOffenderContacts(prisonerId, false)
  }

  @Test
  fun `Get Contact Throws PrisonerNotFoundException for NOT_FOUND`() {
    val prisonerId = "A1234AA"

    whenever(
      apiClient.getOffenderContacts(prisonerId, false),
    ).thenThrow(
      PrisonerNotFoundException(),
    )

    assertThrows<PrisonerNotFoundException> {
      contactService.getContactList(prisonerId, approvedVisitorsOnly = false)
    }

    verify(apiClient, times(1)).getOffenderContacts(prisonerId, false)
  }

  @Test
  fun `Get Contact Throws WebClientResponseException`() {
    val prisonerId = "A1234AA"

    whenever(
      apiClient.getOffenderContacts(prisonerId, false),
    ).thenThrow(
      WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(), "", HttpHeaders.EMPTY, byteArrayOf(), null),
    )

    assertThrows<WebClientResponseException> {
      contactService.getContactList(prisonerId)
    }

    verify(apiClient, times(1)).getOffenderContacts(prisonerId, false)
  }

  @Test
  fun `Get Contact Returns Contact List with Address List`() {
    val prisonerId = "A1234AA"

    whenever(
      apiClient.getOffenderContacts(prisonerId, false),
    ).thenReturn(ContactsDto(listOf(offenderContact)))
    whenever(
      apiClient.getPersonAddress(any()),
    ).thenReturn(listOf(personAddress))

    val contacts = contactService.getContactList(prisonerId)

    assertThat(contacts).isNotNull
    assertThat(contacts).isNotEmpty
    assertThat(contacts[0]).isNotNull
    assertThat(contacts[0].addresses).isNotNull
    assertThat(contacts[0].addresses).isNotEmpty

    verify(apiClient, times(1)).getOffenderContacts(prisonerId, false)
  }

  @Test
  fun `get contact returns addresses when withAddress is true`() {
    val prisonerId = "A1234AA"

    whenever(
      apiClient.getOffenderContacts(prisonerId, false),
    ).thenReturn(ContactsDto(listOf(offenderContact)))
    whenever(
      apiClient.getPersonAddress(any()),
    ).thenReturn(listOf(personAddress))

    val contacts = contactService.getContactList(prisonerId, withAddress = true)

    assertThat(contacts).isNotNull
    assertThat(contacts).isNotEmpty
    assertThat(contacts[0]).isNotNull
    assertThat(contacts[0].addresses).isNotNull
    assertThat(contacts[0].addresses).isNotEmpty

    verify(apiClient, times(1)).getOffenderContacts(prisonerId, false)
  }

  @Test
  fun `get contact returns addresses when withAddress is false`() {
    val prisonerId = "A1234AA"
    offenderContact.addresses = emptyList()

    whenever(
      apiClient.getOffenderContacts(prisonerId, false),
    ).thenReturn(ContactsDto(listOf(offenderContact)))

    whenever(
      apiClient.getPersonAddress(any()),
    ).thenReturn(listOf(personAddress))

    val contacts = contactService.getContactList(prisonerId, withAddress = false)

    assertThat(contacts).isNotNull
    assertThat(contacts).isNotEmpty
    assertThat(contacts[0]).isNotNull
    assertThat(contacts[0].addresses).isEmpty()

    verify(apiClient, times(1)).getOffenderContacts(prisonerId, false)
    verify(apiClient, times(0)).getPersonAddress(any())
  }

  @Test
  fun `Get Contact Returns Contact List with person Not Found`() {
    val prisonerId = "A1234AA"

    whenever(
      apiClient.getOffenderContacts(prisonerId, false),
    ).thenReturn(ContactsDto(listOf(offenderContact)))
    whenever(
      apiClient.getPersonAddress(any()),
    ).thenThrow(
      WebClientResponseException.create(HttpStatus.NOT_FOUND.value(), "", HttpHeaders.EMPTY, byteArrayOf(), null),
    )

    val contacts = contactService.getContactList(prisonerId)

    assertThat(contacts).isNotNull
    assertThat(contacts).isNotEmpty
    assertThat(contacts[0]).isNotNull
    assertThat(contacts[0].addresses).isNotNull
    assertThat(contacts[0].addresses).isEmpty()

    verify(apiClient, times(1)).getOffenderContacts(prisonerId, false)
  }

  @Test
  fun `Get Contact Returns Contact List with person WebClientResponseException`() {
    val prisonerId = "A1234AA"

    whenever(
      apiClient.getOffenderContacts(prisonerId, false),
    ).thenReturn(ContactsDto(listOf(offenderContact)))
    whenever(
      apiClient.getPersonAddress(any()),
    ).thenThrow(
      WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(), "", HttpHeaders.EMPTY, byteArrayOf(), null),
    )

    assertThrows<WebClientResponseException> {
      contactService.getContactList(prisonerId)
    }

    verify(apiClient, times(1)).getOffenderContacts(prisonerId, false)
  }

  @Test
  fun `Get Contact Returns Contact List Filtered By ContactType`() {
    val prisonerId = "A1234AA"
    val contactType = "O"

    whenever(
      apiClient.getOffenderContacts(prisonerId, false),
    ).thenReturn(ContactsDto(listOf(offenderContact)))

    val contacts = contactService.getContactList(prisonerId = prisonerId, contactType = contactType)

    assertThat(contacts).isNotNull
    assertThat(contacts).isNotEmpty

    verify(apiClient, times(1)).getOffenderContacts(prisonerId, false)
  }

  @Test
  fun `Get Contact Returns Empty Contact List Filtered By ContactType`() {
    val prisonerId = "A1234AA"
    val contactType = "Unknown"

    whenever(
      apiClient.getOffenderContacts(prisonerId, false),
    ).thenReturn(ContactsDto(listOf(offenderContact)))

    val contacts = contactService.getContactList(prisonerId = prisonerId, contactType = contactType)

    assertThat(contacts).isNotNull
    assertThat(contacts).isEmpty()

    verify(apiClient, times(1)).getOffenderContacts(prisonerId, false)
  }

  @Test
  fun `Get Contact Returns Contact List Filtered By PersonId`() {
    val prisonerId = "A1234AA"
    val personId: Long = 5871791

    whenever(
      apiClient.getOffenderContacts(prisonerId, false),
    ).thenReturn(ContactsDto(listOf(offenderContact)))

    val contacts = contactService.getContactList(prisonerId = prisonerId, personId = personId)

    assertThat(contacts).isNotNull
    assertThat(contacts).isNotEmpty

    verify(apiClient, times(1)).getOffenderContacts(prisonerId, false)
  }

  @Test
  fun `Get Contact Returns Empty Contact List Filtered By PersonId`() {
    val prisonerId = "A1234AA"
    val personId: Long = 1234567

    whenever(
      apiClient.getOffenderContacts(prisonerId, false),
    ).thenReturn(ContactsDto(listOf(offenderContact)))

    val contacts = contactService.getContactList(prisonerId = prisonerId, personId = personId)

    assertThat(contacts).isNotNull
    assertThat(contacts).isEmpty()

    verify(apiClient, times(1)).getOffenderContacts(prisonerId, false)
  }
}
