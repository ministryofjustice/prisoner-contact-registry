package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PersonalRelationshipsContactDto(
  @param:Schema(description = "Identifier for this contact", example = "5871791")
  val contactId: Long,

  @param:Schema(description = "First name", example = "John", required = true)
  val firstName: String,

  @param:Schema(description = "Middle names", example = "William", required = false)
  val middleNames: String? = null,

  @param:Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,

  @param:Schema(description = "Date of birth", example = "1980-01-28", required = false)
  val dateOfBirth: LocalDate? = null,

  @param:Schema(description = "Code for relationship to Prisoner", example = "FRI", required = true)
  val relationshipToPrisonerCode: String,

  @param:Schema(description = "Description of relationship to Prisoner", example = "Friend", required = false)
  val relationshipToPrisonerDescription: String? = null,

  @param:Schema(description = "Relationship type code", example = "S", required = true)
  val relationshipTypeCode: String,

  @param:Schema(description = "Relationship type description", example = "Friend", required = false)
  val relationshipTypeDescription: String? = null,

  @param:Schema(description = "Approved Visitor Flag", required = true)
  val isApprovedVisitor: Boolean,

  @param:Schema(description = "Emergency Contact Flag", required = true)
  val isEmergencyContact: Boolean,

  @param:Schema(description = "Next of Kin Flag", required = true)
  val isNextOfKin: Boolean,

  @param:Schema(description = "Additional Information", example = "This is a comment text", required = false)
  val comments: String? = null,
)
