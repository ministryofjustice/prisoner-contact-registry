package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.apache.coyote.BadRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.DateRangeDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.HasClosedRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.PrisonerContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.visit.scheduler.RequestVisitVisitorRestrictionsBodyDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.VisitorNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.mappers.IndexedRestrictions
import java.time.LocalDate

@Service
class PrisonerContactFacadeService(
  private val prisonerContactService: PrisonerContactService,
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
    log.debug(
      "getSocialContactList called with parameters : prisonerId - {}, hasDateOfBirth - {}, approvedContactsOnly - {}",
      prisonerId,
      hasDateOfBirth,
      approvedContactsOnly,
    )

    val contacts = getContactsByPrisonerId(
      prisonerId = prisonerId,
      approvedContactsOnly = approvedContactsOnly,
      withRestrictions = withRestrictions,
    )

    return contacts
      .let { if (hasDateOfBirth) it.filter { contact -> contact.dateOfBirth != null } else it }
      .sortedWith(getDefaultSortOrder())
  }

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

    val contacts = getApprovedContactsForVisitorIdsOrThrow(
      prisonerId = prisonerId,
      visitorIds = visitorIds,
      visitorNotFoundMessage = "Not all visitors provided ($visitorIds) are listed contacts for prisoner $prisonerId",
    )

    return restrictionsService.getBannedDateRangeForContacts(
      contacts = contacts,
      fromDate = fromDate,
      toDate = toDate,
    )
  }

  fun getClosedRestrictionStatusForPrisonerContacts(
    prisonerId: String,
    visitorIds: List<Long>,
  ): HasClosedRestrictionDto {
    log.debug(
      "getClosedRestrictionStatusForPrisonerContacts called with parameters : prisonerId - {}, visitorIds - {}",
      prisonerId,
      visitorIds,
    )

    val contacts = getApprovedContactsForVisitorIdsOrThrow(
      prisonerId = prisonerId,
      visitorIds = visitorIds,
      visitorNotFoundMessage = "Not all visitors provided ($visitorIds) are listed contacts for prisoner $prisonerId",
    )

    return restrictionsService.getClosedRestrictionStatusForContacts(contacts)
  }

  fun getDateRangesForVisitorRestrictionsWhichAffectRequestVisits(
    visitBookingDetails: RequestVisitVisitorRestrictionsBodyDto,
  ): List<DateRangeDto> {
    log.info(
      "getDateRangesForVisitorRestrictionsWhichEffectRequestVisits called with request: {}",
      visitBookingDetails,
    )

    val visitorIds = visitBookingDetails.visitorIds.mapNotNull { it.toLongOrNull() }
    if (visitorIds.size != visitBookingDetails.visitorIds.size) {
      throw BadRequestException("One or more visitorIds are not valid numeric IDs: ${visitBookingDetails.visitorIds}")
    }

    val contacts = getApprovedContactsForVisitorIdsOrThrow(
      prisonerId = visitBookingDetails.prisonerId,
      visitorIds = visitorIds,
      visitorNotFoundMessage = "Not all visitors provided (${visitBookingDetails.visitorIds}) are listed contacts for prisoner ${visitBookingDetails.prisonerId}",
    )

    return restrictionsService.getDateRangesForVisitorRestrictionsWhichAffectRequestVisits(
      contacts = contacts,
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
    log.info(
      "getPrisonerContactViaRelationship called with parameters : prisonerId {}, contactId {}, relationshipId {}",
      prisonerId,
      contactId,
      relationshipId,
    )

    val contact = prisonerContactService.getPrisonerContactViaRelationship(
      prisonerId = prisonerId,
      contactId = contactId,
      relationshipId = relationshipId,
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

  private fun getApprovedContactsForVisitorIdsOrThrow(
    prisonerId: String,
    visitorIds: List<Long>,
    visitorNotFoundMessage: String,
  ): List<PrisonerContactDto> {
    val contacts = getContactsByPrisonerId(
      prisonerId = prisonerId,
      approvedContactsOnly = true,
      withRestrictions = true,
    ).filter { it.personId in visitorIds }

    if (!contacts.map { it.personId }.containsAll(visitorIds)) {
      throw VisitorNotFoundException(message = visitorNotFoundMessage)
    }

    return contacts
  }

  private fun getContactsByPrisonerId(
    prisonerId: String,
    approvedContactsOnly: Boolean,
    withRestrictions: Boolean,
  ): List<PrisonerContactDto> {
    val prisonerContacts = prisonerContactService.getPrisonerContacts(
      prisonerId = prisonerId,
      approvedContactsOnly = approvedContactsOnly,
    )

    log.info(
      "Get prisoner contacts called for {}, via the personal-relationships-api returned {} contacts, relationshipType = S, withRestrictions = {}, approvedVisitorOnly = {}",
      prisonerId,
      prisonerContacts.size,
      withRestrictions,
      approvedContactsOnly,
    )

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
