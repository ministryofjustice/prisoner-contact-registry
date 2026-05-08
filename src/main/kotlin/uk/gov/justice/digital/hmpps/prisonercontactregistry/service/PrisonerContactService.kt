package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.DateRangeDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.HasClosedRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.PrisonerContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.visit.scheduler.RequestVisitVisitorRestrictionsBodyDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.VisitorNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.mappers.IndexedRestrictions
import java.time.LocalDate

@Service
class PrisonerContactService(
  private val personalRelationshipsApiClient: PersonalRelationshipsApiClient,
  private val restrictionsService: RestrictionsService,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getSocialContactList(
    prisonerId: String,
    hasDateOfBirth: Boolean,
    approvedContactsOnly: Boolean,
    withRestrictions: Boolean,
  ): List<PrisonerContactDto> {
    log.debug("getSocialContactList called with parameters : prisonerId - {}, hasDateOfBirth - {}, approvedContactsOnly - {}", prisonerId, hasDateOfBirth, approvedContactsOnly)

    var socialContacts = getContactsByPrisonerId(prisonerId, approvedContactsOnly, withRestrictions)

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

    val visitors = getApprovedContactsForVisitorIds(prisonerId, visitorIds)

    return restrictionsService.getBannedDateRangeForContacts(
      contacts = visitors,
      fromDate = fromDate,
      toDate = toDate,
    )
  }

  fun getClosedRestrictionStatusForPrisonerContacts(
    prisonerId: String,
    visitorIds: List<Long>,
  ): HasClosedRestrictionDto {
    log.debug("getClosedRestrictionStatusForPrisonerContacts called with parameters : prisonerId - {}, visitorIds - {}", prisonerId, visitorIds)

    val visitors = getApprovedContactsForVisitorIds(prisonerId, visitorIds)

    return restrictionsService.getClosedRestrictionStatusForContacts(visitors)
  }

  fun getDateRangesForVisitorRestrictionsWhichAffectRequestVisits(
    visitBookingDetails: RequestVisitVisitorRestrictionsBodyDto,
  ): List<DateRangeDto> {
    log.info("getDateRangesForVisitorRestrictionsWhichAffectRequestVisits called with request: {}", visitBookingDetails)

    val visitors = getApprovedContactsForVisitorIds(
      prisonerId = visitBookingDetails.prisonerId,
      visitorIds = visitBookingDetails.visitorIds.map { it.toLong() },
    )

    return restrictionsService.getDateRangesForVisitorRestrictionsWhichAffectRequestVisits(
      contacts = visitors,
      supportedRestrictionTypes = visitBookingDetails.supportedVisitorRestrictionsCodesForRequestVisits,
      currentDateRange = visitBookingDetails.currentDateRange,
    )
  }

  fun getPrisonerContactViaRelationship(
    prisonerId: String,
    contactId: String,
    relationshipId: Long,
    withRestrictions: Boolean,
  ): PrisonerContactDto {
    log.info("getPrisonerContactViaRelationship called with parameters : prisonerId {}, contactId {}, relationshipId {}", prisonerId, contactId, relationshipId)

    val contact = personalRelationshipsApiClient.getPrisonerContactViaRelationshipId(
      prisonerId,
      contactId,
      relationshipId,
    ) ?: throw VisitorNotFoundException(
      message = "Contact with id $contactId not found for prisoner $prisonerId, for relationship (prisonerContactId) $relationshipId",
    )

    val indexedRestrictions = if (withRestrictions) {
      restrictionsService.getContactsGlobalAndLocalRestrictions(listOf(contact.prisonerContactId))
    } else {
      IndexedRestrictions.EMPTY
    }

    return contact.toPrisonerContactDto(
      restrictions = indexedRestrictions.forContact(
        contactId = contact.contactId,
        prisonerContactId = contact.prisonerContactId,
      ),
    )
  }

  private fun getApprovedContactsForVisitorIds(
    prisonerId: String,
    visitorIds: List<Long>,
  ): List<PrisonerContactDto> {
    val contacts = getContactsByPrisonerId(
      prisonerId = prisonerId,
      approvedContactsOnly = true,
      withRestrictions = true,
    ).filter { it.personId in visitorIds }

    if (!contacts.map { it.personId }.containsAll(visitorIds)) {
      throw VisitorNotFoundException(message = "Not all visitors provided ($visitorIds) are listed contacts for prisoner $prisonerId")
    }

    return contacts
  }

  private fun getContactsByPrisonerId(
    prisonerId: String,
    approvedContactsOnly: Boolean,
    withRestrictions: Boolean,
  ): List<PrisonerContactDto> {
    val prisonerContacts = personalRelationshipsApiClient.getPrisonerContacts(prisonerId, approvedContactsOnly)

    log.info("Get prisoner contacts called for {}, via the personal-relationships-api returned {} contacts, relationshipType = S, withRestrictions = {}, approvedVisitorOnly = {}", prisonerId, prisonerContacts.size, withRestrictions, approvedContactsOnly)

    val indexedRestrictions = if (withRestrictions) {
      restrictionsService.getContactsGlobalAndLocalRestrictions(
        prisonerContacts.map { it.prisonerContactId },
      )
    } else {
      IndexedRestrictions.EMPTY
    }

    return prisonerContacts.map { contact ->
      contact.toPrisonerContactDto(
        restrictions = indexedRestrictions.forContact(
          contactId = contact.contactId,
          prisonerContactId = contact.prisonerContactId,
        ),
      )
    }
  }

  private fun getDefaultSortOrder(): Comparator<PrisonerContactDto> = compareBy({ it.lastName }, { it.firstName })
}
