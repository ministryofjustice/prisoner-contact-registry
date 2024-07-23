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
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.HasClosedRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.VisitorActiveRestrictionsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.enum.RestrictionType
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.DateRangeNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.PersonNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.VisitorNotFoundException
import java.time.LocalDate

@Service
class PrisonerContactRegistryService(private val prisonApiClient: PrisonApiClient) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getContactList(
    prisonerId: String,
    contactType: String? = null,
    personId: Long? = null,
    withAddress: Boolean? = true,
    approvedVisitorsOnly: Boolean = false,
  ): List<ContactDto> {
    log.debug("getContactList called with parameters : prisonerId - {}, contactType - {}, personId - {}, withAddress - {}, approvedVisitorsOnly - {}", prisonerId, contactType, personId, withAddress, approvedVisitorsOnly)

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

  fun getSocialContactList(
    prisonerId: String,
    personId: Long? = null,
    withAddress: Boolean,
    hasDateOfBirth: Boolean? = null,
    notBannedBeforeDate: LocalDate? = null,
    approvedVisitorsOnly: Boolean,
  ): List<ContactDto> {
    log.debug("getSocialContactList called with parameters : prisonerId - {}, personId - {}, withAddress - {}, hasDateOfBirth - {}, notBannedBeforeDate - {}, approvedVisitsOnly - {}", prisonerId, personId, withAddress, hasDateOfBirth, notBannedBeforeDate, approvedVisitorsOnly)
    var contacts = getContactList(prisonerId = prisonerId, contactType = "S", personId = personId, withAddress = withAddress, approvedVisitorsOnly = approvedVisitorsOnly)

    if (hasDateOfBirth != null && hasDateOfBirth) {
      contacts = contacts.filter { hasContactGotDateOfBirth(it) }
    }

    if (notBannedBeforeDate != null) {
      contacts = contacts.filterNot { isContactBannedBeforeDate(it, notBannedBeforeDate) }
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
    log.debug(
      "getBannedDateRangeForPrisonerContacts called with parameters : prisonerId - {}, visitorIds - {}, fromDate - {}, toDate - {}",
      prisonerId,
      visitorIds,
      fromDate,
      toDate,
    )

    val dateRange = DateRangeDto(fromDate, toDate)

    val visitorBanRestrictions = getVisitorsWithRestrictionType(prisonerId, visitorIds, RestrictionType.BANNED)

    visitorBanRestrictions.forEach { restriction ->
      restriction.expiryDate?.let { expiryDate ->
        if (expiryDate >= dateRange.toDate) {
          throw DateRangeNotFoundException(message = "Found visitor with restriction of 'BAN' with expiry date after our endDate, no date range possible")
        }

        if (expiryDate > dateRange.fromDate) {
          dateRange.fromDate = expiryDate
        }
      } ?: throw DateRangeNotFoundException("Found visitor with restriction of 'BAN' with no expiry date, no date range possible")
    }

    return dateRange
  }

  @Throws(VisitorNotFoundException::class)
  fun getClosedRestrictionStatusForPrisonerContacts(prisonerId: String, visitorIds: List<Long>): HasClosedRestrictionDto {
    log.debug(
      "getHasClosedRestrictionForPrisonerContacts called with parameters : prisonerId - {}, visitorIds - {}",
      prisonerId,
      visitorIds,
    )

    val visitorClosedRestrictions = getVisitorsWithRestrictionType(prisonerId, visitorIds, RestrictionType.CLOSED)

    return HasClosedRestrictionDto(
      visitorClosedRestrictions.any { restriction ->
        restriction.expiryDate == null || LocalDate.now() <= restriction.expiryDate
      },
    )
  }

  @Throws(VisitorNotFoundException::class)
  fun getActiveRestrictionsForPrisonerContact(prisonerId: String, visitorId: Long): VisitorActiveRestrictionsDto {
    log.debug(
      "getActiveRestrictionsForPrisonerContact called with parameters : prisonerId - {}, visitorId - {}",
      prisonerId,
      visitorId,
    )
    val visitor = getVisitor(prisonerId, visitorId)
    val visitorActiveRestrictions =
      visitor.restrictions.filter {
        it.expiryDate == null || it.expiryDate.isEqual(LocalDate.now()) || it.expiryDate.isAfter(LocalDate.now())
      }.map { it.restrictionType }

    return VisitorActiveRestrictionsDto(visitorActiveRestrictions)
  }

  @Throws(PrisonerNotFoundException::class)
  private fun getContactById(id: String, approvedVisitorsOnly: Boolean): List<ContactDto> {
    try {
      return prisonApiClient.getOffenderContacts(id, approvedVisitorsOnly).offenderContacts
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
    return restriction.restrictionType == RestrictionType.BANNED.toString() &&
      isBannedForDate(restriction.expiryDate, date)
  }

  private fun isBannedForDate(restrictionEndDate: LocalDate?, date: LocalDate): Boolean {
    return (restrictionEndDate == null || (date <= restrictionEndDate))
  }

  private fun getVisitors(prisonerId: String, visitorIds: List<Long>): List<ContactDto> {
    val contacts = getContactById(prisonerId, true)

    val visitors = contacts.filter { visitorIds.contains(it.personId) }
    if (visitors.size != visitorIds.size) {
      throw VisitorNotFoundException(message = "Not all visitors provided ($visitorIds) are listed contacts for prisoner $prisonerId")
    }

    return visitors
  }

  private fun getVisitorsWithRestrictionType(prisonerId: String, visitorIds: List<Long>, restrictionType: RestrictionType): List<RestrictionDto> {
    val visitors = getVisitors(prisonerId, visitorIds)
    val visitorsWithRestriction =
      visitors
        .flatMap { it.restrictions }
        .filter { it.restrictionType == restrictionType.toString() }

    return visitorsWithRestriction
  }

  private fun getVisitor(prisonerId: String, visitorId: Long): ContactDto {
    val contacts = getContactById(prisonerId, true)

    val visitor = contacts.firstOrNull { it.personId == visitorId }
    if (visitor == null) {
      throw VisitorNotFoundException(message = "visitor provided ($visitorId) is not listed contact for prisoner $prisonerId")
    }

    return visitor
  }
}
