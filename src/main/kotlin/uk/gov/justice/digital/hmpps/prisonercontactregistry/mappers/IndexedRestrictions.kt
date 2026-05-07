package uk.gov.justice.digital.hmpps.prisonercontactregistry.mappers

import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
import java.time.LocalDate

data class IndexedRestrictions(
  private val localByPrisonerContactId: Map<Long, List<RestrictionDto>>,
  private val globalByContactId: Map<Long, List<RestrictionDto>>,
) {
  fun forContact(contactId: Long, prisonerContactId: Long?): List<RestrictionDto> {
    val local = prisonerContactId?.let { localByPrisonerContactId[it] }.orEmpty()
    val global = globalByContactId[contactId].orEmpty()

    return local + global
  }

  companion object {
    val EMPTY = IndexedRestrictions(
      localByPrisonerContactId = emptyMap(),
      globalByContactId = emptyMap(),
    )
  }
}

fun PrisonerContactRestrictionsResponseDto?.toIndexedRestrictions(
  today: LocalDate = LocalDate.now(),
): IndexedRestrictions {
  if (this == null) {
    return IndexedRestrictions.EMPTY
  }

  val localByPrisonerContactId = prisonerContactRestrictions
    .associate { group ->
      group.prisonerContactId to group.prisonerContactRestrictions
        .filter { it.expiryDate.isActiveOn(today) }
        .map { RestrictionDto(it) }
    }

  val globalByContactId = prisonerContactRestrictions
    .asSequence()
    .flatMap { it.globalContactRestrictions.asSequence() }
    .distinctBy { it.contactRestrictionId }
    .filter { it.expiryDate.isActiveOn(today) }
    .groupBy(
      keySelector = { it.contactId },
      valueTransform = { RestrictionDto(it) },
    )

  return IndexedRestrictions(
    localByPrisonerContactId = localByPrisonerContactId,
    globalByContactId = globalByContactId,
  )
}

private fun LocalDate?.isActiveOn(today: LocalDate): Boolean = this == null || !today.isAfter(this)
