package uk.gov.justice.digital.hmpps.prisonercontactregistry.mappers

import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.GlobalContactRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
import java.time.LocalDate

data class IndexedRestrictions(
  val localByPrisonerContactId: Map<Long, List<RestrictionDto>>,
  val globalByContactId: Map<Long, List<RestrictionDto>>,
) {
  companion object {
    val EMPTY = IndexedRestrictions(
      localByPrisonerContactId = emptyMap(),
      globalByContactId = emptyMap(),
    )
  }

  fun forContact(contactId: Long, prisonerContactId: Long?): List<RestrictionDto> {
    val local = prisonerContactId?.let { localByPrisonerContactId[it] }.orEmpty()
    val global = globalByContactId[contactId].orEmpty()

    return local + global
  }
}

fun PrisonerContactRestrictionsResponseDto?.toIndexedRestrictions(today: LocalDate = LocalDate.now()): IndexedRestrictions {
  if (this == null) {
    return IndexedRestrictions.EMPTY
  }

  val localByPrisonerContactId = prisonerContactRestrictions
    .associate { group ->
      group.prisonerContactId to group.prisonerContactRestrictions
        .filter { it.expiryDate.isActiveOn(today) }
        .map { it.toRestrictionDto() }
    }

  val globalByContactId = prisonerContactRestrictions
    .asSequence()
    .flatMap { it.globalContactRestrictions.asSequence() }
    .distinctBy { it.contactRestrictionId }
    .filter { it.expiryDate.isActiveOn(today) }
    .groupBy(
      keySelector = { it.contactId },
      valueTransform = { it.toRestrictionDto() },
    )

  return IndexedRestrictions(
    localByPrisonerContactId = localByPrisonerContactId,
    globalByContactId = globalByContactId,
  )
}

fun PrisonerContactRestrictionDto.toRestrictionDto(): RestrictionDto = RestrictionDto(
  restrictionId = prisonerContactRestrictionId.toInt(),
  restrictionType = restrictionType,
  restrictionTypeDescription = restrictionTypeDescription,
  startDate = startDate,
  expiryDate = expiryDate,
  globalRestriction = false,
  comment = comments,
)

fun GlobalContactRestrictionDto.toRestrictionDto(): RestrictionDto = RestrictionDto(
  restrictionId = contactRestrictionId.toInt(),
  restrictionType = restrictionType,
  restrictionTypeDescription = restrictionTypeDescription,
  startDate = startDate,
  expiryDate = expiryDate,
  globalRestriction = true,
  comment = comments,
)

private fun LocalDate?.isActiveOn(today: LocalDate): Boolean = this == null || !today.isAfter(this)
