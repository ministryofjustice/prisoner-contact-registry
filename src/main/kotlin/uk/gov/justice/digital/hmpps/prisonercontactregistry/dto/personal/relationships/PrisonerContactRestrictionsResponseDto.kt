package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships

import java.time.LocalDate

data class PrisonerContactRestrictionsResponseDto(
  val prisonerContactRestrictions: List<PrisonerContactRestrictionsDto>,
)

data class PrisonerContactRestrictionsDto(
  val prisonerContactId: Long,
  val prisonerContactRestrictions: List<PrisonerContactRestrictionDto>,
  val globalContactRestrictions: List<GlobalContactRestrictionDto>,
)

data class PrisonerContactRestrictionDto(
  val prisonerContactRestrictionId: Long,
  val prisonerContactId: Long,
  val contactId: Long,
  val prisonerNumber: String,
  val restrictionType: String,
  val restrictionTypeDescription: String,
  val startDate: LocalDate,
  val expiryDate: LocalDate?,
  val comments: String?,
  val enteredByDisplayName: String,
)

data class GlobalContactRestrictionDto(
  val contactRestrictionId: Long,
  val contactId: Long,
  val restrictionType: String,
  val restrictionTypeDescription: String,
  val startDate: LocalDate,
  val expiryDate: LocalDate?,
  val comments: String?,
  val enteredByDisplayName: String,
)
