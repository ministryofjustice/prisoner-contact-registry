package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.AddressDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.DateRangeDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.DateRangeNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.PersonNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.VisitorNotFoundException
import java.time.LocalDate

@Service
class PrisonerContactRegistryService(private val prisonApiClient: PrisonApiClient) {

  fun getContactList(
    prisonerId: String,
    contactType: String? = null,
    personId: Long? = null,
    withAddress: Boolean? = true,
  ): List<ContactDto> {
    // Prisoners (Offenders) have a subset of Contacts (Persons / Visitors) which can be filtered by type and person id.
    // When filtering the offenders contacts by type of person this results in a query result (list of matching contacts
    // or empty list) If a prisoner is not found this results in a PrisonerNotFoundException (404).

    var contacts = getContactById(prisonerId)

    if (personId != null) {
      contacts = filterByPersonId(contacts, personId)
    }

    if (!contactType.isNullOrBlank()) {
      contacts = filterByContactType(contacts, contactType)
    }

    // only get contacts address if needed, true by default
    val getAddresses = withAddress ?: true
    if (getAddresses) {
      contacts.forEach {
        try {
          // In NOMIS a contact does not require a personId
          it.addresses = it.personId?.let { id -> getAddressesById(id) } ?: emptyList()
        } catch (e: PersonNotFoundException) {
          // Nomis data quality issue - treat as no address data available
          log.warn("Person not found for prisoner $prisonerId contact ${it.personId}")
          it.addresses = emptyList()
        }
      }
    }
    return contacts
  }

  @Throws(VisitorNotFoundException::class, DateRangeNotFoundException::class)
  fun getBannedDateRangeForPrisonerContacts(
    prisonerId: String,
    visitorIds: List<Long>,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): DateRangeDto {
    val dateRange = DateRangeDto(fromDate, toDate)

    val contacts = getContactById(prisonerId)
    val visitors = contacts.filter { visitorIds.contains(it.personId) }
    if (visitors.size != visitorIds.size) {
      throw VisitorNotFoundException(message = "Not all visitors provided ($visitorIds) are listed contacts for prisoner $prisonerId")
    }

    val visitorBanRestrictions = visitors
      .flatMap { it.restrictions }
      .filter { it.restrictionType == "BAN" }

    for (restriction in visitorBanRestrictions) {
      restriction.expiryDate?.let { expiryDate ->
        if (expiryDate.isAfter(dateRange.toDate)) {
          dateRange.toDate = expiryDate
        }
      } ?: run {
        // If an expiry date is found to be null, it is classed as an "open-ended" ban. Thus, no suitable date range can be given.
        // Replace with new DateRangeNotFoundException.
        throw DateRangeNotFoundException(message = "Found visitor with restriction of 'BAN' with no expiry date, no date range possible")
      }
    }

    return dateRange
  }

  @Throws(PrisonerNotFoundException::class)
  private fun getContactById(id: String): List<ContactDto> {
    try {
      return prisonApiClient.getOffenderContacts(id)!!.offenderContacts
    } catch (e: WebClientResponseException) {
      if (e.statusCode == HttpStatus.NOT_FOUND) {
        throw PrisonerNotFoundException(e.message, e)
      }
      throw e
    }
  }

  @Throws(PersonNotFoundException::class)
  private fun getAddressesById(id: Long): List<AddressDto> {
    try {
      return prisonApiClient.getPersonAddress(id)!!
    } catch (e: WebClientResponseException) {
      if (e.statusCode == HttpStatus.NOT_FOUND) {
        throw PersonNotFoundException(e.message, e)
      }
      throw e
    }
  }

  private fun filterByPersonId(contacts: List<ContactDto>?, personId: Long): List<ContactDto> {
    return contacts?.filter { it.personId == personId } ?: emptyList()
  }

  private fun filterByContactType(contacts: List<ContactDto>?, contactType: String): List<ContactDto> {
    return contacts?.filter { it.contactType.equals(contactType, true) } ?: emptyList()
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
