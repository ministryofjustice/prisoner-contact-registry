package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.AddressDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import java.time.LocalDate
import java.util.function.Supplier

@Service
class PrisonerContactRegistryService(private val prisonApiClient: PrisonApiClient) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val BANNED_RESTRICTION_TYPE = "BAN"
  }

  fun getContactList(prisonerId: String, contactType: String? = null, personId: Long? = null, withAddress: Boolean? = true, approvedVisitorsOnly: Boolean? = null): List<ContactDto> {
    // Prisoners (Offenders) have a subset of Contacts (Persons / Visitors) which can be filtered by type and person id.
    // When filtering the offenders contacts by type of person this results in a query result (list of matching contacts
    // or empty list) If a prisoner is not found this results in a PrisonerNotFoundException (404).

    var contacts = getContactById(prisonerId, approvedVisitorsOnly)

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

  fun getApprovedSocialContactList(
    prisonerId: String,
    personId: Long? = null,
    withAddress: Boolean,
    hasDateOfBirth: Boolean? = null,
    notBannedBeforeDate: LocalDate? = null,
  ): List<ContactDto> {
    log.debug("getContactList called with parameters : prisonerId - {}, personId - {}, withAddress - {}, hasDateOfBirth - {}, notBannedBeforeDate - {}", prisonerId, personId, withAddress, hasDateOfBirth, notBannedBeforeDate)
    var contacts = getContactList(prisonerId = prisonerId, contactType = "S", personId = personId, withAddress = withAddress, approvedVisitorsOnly = true)

    if (hasDateOfBirth != null && hasDateOfBirth) {
      contacts = contacts.filter { hasContactGotDateOfBirth(it) }
    }

    if (notBannedBeforeDate != null) {
      contacts = contacts.filterNot { isContactBannedBeforeDate(it, notBannedBeforeDate) }
    }

    return contacts
  }

  @Throws(PrisonerNotFoundException::class)
  private fun getContactById(id: String, approvedVisitorsOnly: Boolean?): List<ContactDto> {
    try {
      return prisonApiClient.getOffenderContacts(id, approvedVisitorsOnly)!!.offenderContacts
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

  private fun hasContactGotDateOfBirth(contact: ContactDto): Boolean {
    return contact.dateOfBirth != null
  }

  private fun isContactBannedBeforeDate(contact: ContactDto, date: LocalDate): Boolean {
    return contact.restrictions.any { hasBanForDate(it, date) }
  }

  private fun hasBanForDate(restriction: RestrictionDto, date: LocalDate): Boolean {
    return restriction.restrictionType == BANNED_RESTRICTION_TYPE &&
      isBannedForDate(restriction.expiryDate, date)
  }

  private fun isBannedForDate(restrictionEndDate: LocalDate?, date: LocalDate): Boolean {
    return (restrictionEndDate == null || !(restrictionEndDate.isBefore(date)))
  }
}

class PrisonerNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<PrisonerNotFoundException> {
  override fun get(): PrisonerNotFoundException {
    return PrisonerNotFoundException(message, cause)
  }
}

class PersonNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<PersonNotFoundException> {
  override fun get(): PersonNotFoundException {
    return PersonNotFoundException(message, cause)
  }
}
