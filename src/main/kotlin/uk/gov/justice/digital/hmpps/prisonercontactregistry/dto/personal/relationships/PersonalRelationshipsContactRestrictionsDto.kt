package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships

import io.swagger.v3.oas.annotations.media.Schema

data class PersonalRelationshipsContactRestrictionsDto(
  @param:Schema(description = "Identifier for this contact", example = "5871791")
  val contactId: Long,
)
