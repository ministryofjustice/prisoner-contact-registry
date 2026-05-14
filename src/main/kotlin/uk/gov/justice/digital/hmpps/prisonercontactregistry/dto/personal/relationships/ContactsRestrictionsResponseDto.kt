package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships

data class ContactsRestrictionsResponseDto(
  val contactRestrictions: List<ContactRestrictionsDto>,
)

data class ContactRestrictionsDto(
  val contactId: Long,
  val globalContactRestrictions: List<GlobalContactRestrictionDto>,
)
