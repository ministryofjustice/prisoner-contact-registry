package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships

import java.time.LocalDate

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
