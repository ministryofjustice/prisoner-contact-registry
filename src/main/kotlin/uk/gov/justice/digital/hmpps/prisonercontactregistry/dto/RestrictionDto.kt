package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.GlobalContactRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionDto
import java.time.LocalDate

@Schema(description = "A contact for a prisoner")
data class RestrictionDto(
  @param:Schema(description = "Restriction ID", example = "123", required = true)
  val restrictionId: Int,
  @param:Schema(description = "Restriction Type Code", example = "123", required = true)
  val restrictionType: String,
  @param:Schema(description = "Description of Restriction Type", example = "123", required = true)
  val restrictionTypeDescription: String,
  @param:Schema(description = "Date from which the restriction applies", example = "2000-10-31", required = true)
  val startDate: LocalDate,
  @param:Schema(description = "Restriction Expiry", example = "2000-10-31", required = false)
  val expiryDate: LocalDate? = null,
  @param:Schema(description = "True if applied globally to the contact or False if applied in the context of a visit", required = true)
  val globalRestriction: Boolean,
  @param:Schema(description = "Additional Information", example = "This is a comment text", required = false)
  val comment: String? = null,
) {
  constructor(personalRelationshipsLocalRestriction: PrisonerContactRestrictionDto) : this(
    restrictionId = personalRelationshipsLocalRestriction.prisonerContactRestrictionId.toInt(),
    restrictionType = personalRelationshipsLocalRestriction.restrictionType,
    restrictionTypeDescription = personalRelationshipsLocalRestriction.restrictionTypeDescription,
    startDate = personalRelationshipsLocalRestriction.startDate,
    expiryDate = personalRelationshipsLocalRestriction.expiryDate,
    globalRestriction = false,
    comment = personalRelationshipsLocalRestriction.comments,
  )

  constructor(personalRelationshipsGlobalRestriction: GlobalContactRestrictionDto) : this(
    restrictionId = personalRelationshipsGlobalRestriction.contactRestrictionId.toInt(),
    restrictionType = personalRelationshipsGlobalRestriction.restrictionType,
    restrictionTypeDescription = personalRelationshipsGlobalRestriction.restrictionTypeDescription,
    startDate = personalRelationshipsGlobalRestriction.startDate,
    expiryDate = personalRelationshipsGlobalRestriction.expiryDate,
    globalRestriction = true,
    comment = personalRelationshipsGlobalRestriction.comments,
  )
}
