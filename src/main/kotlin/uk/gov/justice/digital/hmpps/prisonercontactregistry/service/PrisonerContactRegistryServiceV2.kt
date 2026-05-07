package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PersonalRelationshipsApiClient.Companion.logger
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.DateRangeDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.HasClosedRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.PrisonerContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsPrisonerContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.visit.scheduler.RequestVisitVisitorRestrictionsBodyDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.enum.RestrictionType
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.DateRangeNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.VisitorNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.mappers.toIndexedRestrictions
import uk.gov.justice.digital.hmpps.prisonercontactregistry.mappers.toPrisonerContactDto
import java.time.LocalDate

@Service
class PrisonerContactRegistryServiceV2(private val personalRelationshipsApiClient: PersonalRelationshipsApiClient) {
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

    val dateRange = DateRangeDto(fromDate, toDate)

    val allFoundBannedRestrictions = getContactsRestrictionDetails(prisonerId, visitorIds, RestrictionType.BANNED)

    allFoundBannedRestrictions.forEach { restriction ->
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

    val allFoundClosedRestrictions = getContactsRestrictionDetails(prisonerId, visitorIds, RestrictionType.CLOSED)

    return HasClosedRestrictionDto(
      allFoundClosedRestrictions.any { restriction ->
        restriction.expiryDate == null || LocalDate.now() <= restriction.expiryDate
      },
    )
  }

  fun getDateRangesForVisitorRestrictionsWhichEffectRequestVisits(visitBookingDetails: RequestVisitVisitorRestrictionsBodyDto): List<DateRangeDto> {
    log.info("getDateRangesForVisitorRestrictionsWhichAffectRequestVisits called with request: {}", visitBookingDetails)

    // 1. Get & validate visitors
    val visitors = getContactsByPrisonerId(prisonerId = visitBookingDetails.prisonerId, approvedContactsOnly = true, withRestrictions = true)
      .filter { it.personId.toString() in visitBookingDetails.visitorIds }
    if (visitors.size != visitBookingDetails.visitorIds.size) {
      throw VisitorNotFoundException(
        "Not all visitors provided (${visitBookingDetails.visitorIds}) are contacts for prisoner ${visitBookingDetails.prisonerId}",
      )
    }

    // 2. Collect only the “request visit” restrictions
    val restrictions = visitors
      .flatMap { it.restrictions }
      .filter { it.restrictionType in visitBookingDetails.supportedVisitorRestrictionsCodesForRequestVisits }

    if (restrictions.isEmpty()) {
      return emptyList()
    }

    // 3. If there are permanent restrictions, handle them
    restrictions
      .filter { it.expiryDate == null }
      .takeIf { it.isNotEmpty() }
      ?.let { permanentRestrictions ->
        // find the earliest permanent start AFTER the current booking window’s fromDate
        val earliestAfterDate = permanentRestrictions
          .map { it.startDate }
          .filter { it.isAfter(visitBookingDetails.currentDateRange.fromDate) }
          .minOrNull()

        return if (earliestAfterDate != null) {
          listOf(DateRangeDto(fromDate = earliestAfterDate, toDate = visitBookingDetails.currentDateRange.toDate))
        } else {
          listOf(visitBookingDetails.currentDateRange)
        }
      }

    // 4. Else return all dated restrictions without duplications
    return restrictions
      .filter { it.expiryDate != null }
      .map { DateRangeDto(it.startDate, it.expiryDate!!) }
      .distinct()
      .toList()
  }

  fun getPrisonerContactViaRelationship(
    prisonerId: String,
    contactId: String,
    relationshipId: Long,
    withRestrictions: Boolean,
  ): PrisonerContactDto {
    log.info("getPrisonerContactViaRelationship called with parameters : prisonerId $prisonerId, contactId $contactId, relationshipId $relationshipId")

    val contact = personalRelationshipsApiClient.getPrisonerContactViaRelationshipId(prisonerId, contactId, relationshipId)
    if (contact == null) {
      throw VisitorNotFoundException(message = "Contact with id $contactId not found for prisoner $prisonerId, for relationship (prisonerContactId) $relationshipId")
    }

    val allPrisonerContactRestrictions = if (withRestrictions) {
      personalRelationshipsApiClient.getPrisonerContactRestrictions(listOf(contact.prisonerContactId))
    } else {
      null
    }

    val prisonerContact = convertToContactDto(listOf(contact), allPrisonerContactRestrictions).firstOrNull()
    if (prisonerContact == null) {
      throw VisitorNotFoundException(message = "Contact with id $contactId not found for prisoner $prisonerId, for relationship (prisonerContactId) $relationshipId")
    }

    return prisonerContact
  }

  private fun getContactsRestrictionDetails(prisonerId: String, visitorIds: List<Long>, restrictionType: RestrictionType): List<RestrictionDto> {
    val contacts = getContactsByPrisonerId(prisonerId = prisonerId, approvedContactsOnly = true, withRestrictions = true)
      .filter { visitorIds.contains(it.personId) }

    if (!contacts.map { it.personId }.containsAll(visitorIds)) {
      throw VisitorNotFoundException(message = "Not all visitors provided ($visitorIds) are listed contacts for prisoner $prisonerId")
    }

    return contacts
      .flatMap { it.restrictions }
      .filter { it.restrictionType == restrictionType.toString() }
  }

  private fun getContactsByPrisonerId(prisonerId: String, approvedContactsOnly: Boolean, withRestrictions: Boolean): List<PrisonerContactDto> {
    val prisonerContacts = personalRelationshipsApiClient.getPrisonerContacts(prisonerId, approvedContactsOnly)

    logger.info("Get prisoner contacts called for $prisonerId, via the personal-relationships-api returned ${prisonerContacts.size} contacts, relationshipType = S, withRestrictions = $withRestrictions, approvedVisitorOnly = $approvedContactsOnly")

    val allPrisonerContactRestrictions = if (withRestrictions) {
      personalRelationshipsApiClient.getPrisonerContactRestrictions(prisonerContacts.map { it.prisonerContactId })
    } else {
      null
    }

    return convertToContactDto(prisonerContacts, allPrisonerContactRestrictions)
  }

  /**
   * Builds ContactDto entries from Personal Relationships API data to preserve the contract we have with calling APIs.
   *
   * Notes:
   * - The same contactId can appear multiple times, each containing a different
   *   relationship to the prisoner (identified by prisonerContactId).
   * - Restrictions can be relationship-level (local) or contact-level (global).
   *
   * This method:
   * - Attaches local restrictions to the correct relationship using prisonerContactId.
   * - Attaches global restrictions to all relationships for the same contactId.
   * - Preserves duplicate contact entries where relationships differ.
   *
   * Returns:
   * - A ContactDto list [to keep the exact structure as the previous client prison-api had]
   */
  private fun convertToContactDto(
    prisonerContactsList: List<PersonalRelationshipsPrisonerContactDto>,
    prisonerContactRestrictions: PrisonerContactRestrictionsResponseDto?,
  ): List<PrisonerContactDto> {
    val indexedRestrictions = prisonerContactRestrictions.toIndexedRestrictions()

    return prisonerContactsList.map { contact ->
      contact.toPrisonerContactDto(
        restrictions = indexedRestrictions.forContact(
          contactId = contact.contactId,
          prisonerContactId = contact.prisonerContactId,
        ),
      )
    }
  }

  private final fun getDefaultSortOrder(): Comparator<PrisonerContactDto> = compareBy({ it.lastName }, { it.firstName })
}
