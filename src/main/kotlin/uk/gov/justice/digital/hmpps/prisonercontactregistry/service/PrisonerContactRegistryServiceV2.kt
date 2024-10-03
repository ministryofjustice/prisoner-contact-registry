package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PrisonApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.AddressDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.DateRangeDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.HasClosedRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.enum.RestrictionType
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.DateRangeNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.PersonNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.VisitorNotFoundException
import java.time.LocalDate

@Service
class PrisonerContactRegistryServiceV2(private val prisonApiClient: PrisonApiClient) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getSocialContactList(
    prisonerId: String,
    withAddress: Boolean,
    hasDateOfBirth: Boolean,
    approvedContactsOnly: Boolean,
  ): List<ContactDto> {
    log.debug("getSocialContactList called with parameters : prisonerId - {}, withAddress - {}, hasDateOfBirth - {}, approvedContactsOnly - {}", prisonerId, withAddress, hasDateOfBirth, approvedContactsOnly)

    var socialContacts = getContactsByPrisonerId(prisonerId, approvedContactsOnly)
      .filter { it.contactType.equals("S", true) }

    if (withAddress) {
      socialContacts.forEach {
        try {
          // In NOMIS a contact does not require a personId
          it.addresses = it.personId?.let { id -> getAddressesByContactId(id) } ?: emptyList()
        } catch (e: PersonNotFoundException) {
          // Nomis data quality issue - treat as no address data available
          log.warn("Person not found for prisoner $prisonerId contact ${it.personId}")
          it.addresses = emptyList()
        }
      }
    }

    if (hasDateOfBirth) {
      socialContacts = socialContacts.filter { it.dateOfBirth != null }
    }

    return socialContacts.sortedWith(getDefaultSortOrder())
  }

  fun getBannedDateRangeForPrisonerContacts(
    prisonerId: String,
    visitorIds: List<Long>,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): DateRangeDto {
    log.debug("getBannedDateRangeForPrisonerContacts called with parameters : prisonerId - {}, visitorIds - {}, fromDate - {}, toDate - {}", prisonerId, visitorIds, fromDate, toDate)

    val dateRange = DateRangeDto(fromDate, toDate)

    val contactsWithBanRestrictions = getContactsWithRestrictionType(prisonerId, visitorIds, RestrictionType.BANNED)

    contactsWithBanRestrictions.forEach { restriction ->
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

  fun getClosedRestrictionStatusForPrisonerContacts(prisonerId: String, visitorIds: List<Long>): HasClosedRestrictionDto {
    log.debug(
      "getHasClosedRestrictionForPrisonerContacts called with parameters : prisonerId - {}, visitorIds - {}",
      prisonerId,
      visitorIds,
    )

    val contactsWithClosedRestrictions = getContactsWithRestrictionType(prisonerId, visitorIds, RestrictionType.CLOSED)

    return HasClosedRestrictionDto(
      contactsWithClosedRestrictions.any { restriction ->
        restriction.expiryDate == null || LocalDate.now() <= restriction.expiryDate
      },
    )
  }

  private fun getAddressesByContactId(contactId: Long): List<AddressDto> {
    return prisonApiClient.getPersonAddress(contactId)!!
  }

  private fun getContactsWithRestrictionType(prisonerId: String, visitorIds: List<Long>, restrictionType: RestrictionType): List<RestrictionDto> {
    val contacts = getContactsByPrisonerId(prisonerId, true)
      .filter { visitorIds.contains(it.personId) }

    if (contacts.size != visitorIds.size) {
      throw VisitorNotFoundException(message = "Not all visitors provided ($visitorIds) are listed contacts for prisoner $prisonerId")
    }

    return contacts
      .flatMap { it.restrictions }
      .filter { it.restrictionType == restrictionType.toString() }
  }

  private fun getContactsByPrisonerId(prisonerId: String, approvedContactsOnly: Boolean): List<ContactDto> {
    return prisonApiClient.getOffenderContacts(prisonerId, approvedContactsOnly).offenderContacts
  }

  private final fun getDefaultSortOrder(): Comparator<ContactDto> {
    return compareBy({ it.lastName }, { it.firstName })
  }
}
