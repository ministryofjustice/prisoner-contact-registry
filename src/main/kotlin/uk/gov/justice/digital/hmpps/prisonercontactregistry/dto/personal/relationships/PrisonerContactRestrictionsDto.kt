package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships

data class PrisonerContactRestrictionsDto(
  val prisonerContactId: Long,
  val prisonerContactRestrictions: List<PrisonerContactRestrictionDto>,
  val globalContactRestrictions: List<GlobalContactRestrictionDto>,
)
