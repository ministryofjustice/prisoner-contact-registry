package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships

import java.time.LocalDate

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
