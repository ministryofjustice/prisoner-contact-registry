package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.AddressDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import java.util.function.Supplier

@Service
class PrisonerContactRegistryService(private val prisonApiClient: PrisonApiClient) {

  fun getContactList(prisonerId: String, contactType: String? = null, personId: Long? = null): List<ContactDto> {
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
    return contacts
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
