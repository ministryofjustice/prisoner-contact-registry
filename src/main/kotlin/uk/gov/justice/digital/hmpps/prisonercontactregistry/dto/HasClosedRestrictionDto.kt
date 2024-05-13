package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Boolean flag signifying if any visitors have closed restrictions")
class HasClosedRestrictionDto(
  @Schema(description = "Has closed restriction")
  var value: Boolean,
)
