package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.DateRangeDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.HasClosedRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.PrisonerContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.enum.RestrictionType
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.DateRangeNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.mappers.IndexedRestrictions
import uk.gov.justice.digital.hmpps.prisonercontactregistry.mappers.toIndexedRestrictions
import uk.gov.justice.digital.hmpps.prisonercontactregistry.mappers.toRestrictionDto
import java.time.LocalDate

@Service
class RestrictionsService(private val personalRelationshipsApiClient: PersonalRelationshipsApiClient) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getContactGlobalRestrictions(contactId: Long): List<RestrictionDto> {
    log.debug("RestrictionsService - getContactGlobalRestrictions called with parameters : contactId {}", contactId)

    return personalRelationshipsApiClient.getContactGlobalRestrictions(contactId).map { it.toRestrictionDto() }
  }

  fun getContactsGlobalAndLocalRestrictions(
    prisonerContactRelationshipIds: List<Long>,
  ): IndexedRestrictions {
    if (prisonerContactRelationshipIds.isEmpty()) {
      return IndexedRestrictions.EMPTY
    }

    val prisonerContactRestrictions = personalRelationshipsApiClient
      .getPrisonerContactRestrictions(prisonerContactRelationshipIds)

    return prisonerContactRestrictions.toIndexedRestrictions()
  }

  fun getBannedDateRangeForContacts(
    contacts: List<PrisonerContactDto>,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): DateRangeDto {
    val dateRange = DateRangeDto(fromDate, toDate)

    val bannedRestrictions = contacts.getRestrictionsOfType(RestrictionType.BANNED)

    bannedRestrictions.forEach { restriction ->
      val expiryDate = restriction.expiryDate
        ?: throw DateRangeNotFoundException("Found visitor with restriction of 'BAN' with no expiry date, no date range possible")

      if (expiryDate >= dateRange.toDate) {
        throw DateRangeNotFoundException(message = "Found visitor with restriction of 'BAN' with expiry date after our endDate, no date range possible")
      }

      if (expiryDate > dateRange.fromDate) {
        dateRange.fromDate = expiryDate
      }
    }

    return dateRange
  }

  fun getClosedRestrictionStatusForContacts(contacts: List<PrisonerContactDto>): HasClosedRestrictionDto {
    val hasClosedRestriction = contacts
      .getRestrictionsOfType(RestrictionType.CLOSED)
      .any { restriction ->
        restriction.expiryDate == null || LocalDate.now() <= restriction.expiryDate
      }

    return HasClosedRestrictionDto(hasClosedRestriction)
  }

  fun getDateRangesForVisitorRestrictionsWhichAffectRequestVisits(
    contacts: List<PrisonerContactDto>,
    supportedRestrictionTypes: List<String>,
    currentDateRange: DateRangeDto,
  ): List<DateRangeDto> {
    val restrictions = contacts
      .flatMap { it.restrictions }
      .filter { it.restrictionType in supportedRestrictionTypes }

    if (restrictions.isEmpty()) {
      return emptyList()
    }

    val permanentRestrictions = restrictions.filter { it.expiryDate == null }

    if (permanentRestrictions.isNotEmpty()) {
      val earliestPermanentRestrictionStartAfterCurrentFromDate = permanentRestrictions
        .map { it.startDate }
        .filter { it.isAfter(currentDateRange.fromDate) }
        .minOrNull()

      return if (earliestPermanentRestrictionStartAfterCurrentFromDate != null) {
        listOf(
          DateRangeDto(
            fromDate = earliestPermanentRestrictionStartAfterCurrentFromDate,
            toDate = currentDateRange.toDate,
          ),
        )
      } else {
        listOf(currentDateRange)
      }
    }

    return restrictions
      .filter { it.expiryDate != null }
      .map { DateRangeDto(it.startDate, it.expiryDate!!) }
      .distinct()
  }

  private fun List<PrisonerContactDto>.getRestrictionsOfType(restrictionType: RestrictionType): List<RestrictionDto> = flatMap { it.restrictions }.filter { it.restrictionType == restrictionType.toString() }
}
