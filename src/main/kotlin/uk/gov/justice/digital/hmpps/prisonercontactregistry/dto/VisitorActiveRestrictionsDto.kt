package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A list of active restrictions for a visitor")
class VisitorActiveRestrictionsDto(
  @Schema(description = "All active restrictions")
  val activeRestrictions: List<String>,
)
