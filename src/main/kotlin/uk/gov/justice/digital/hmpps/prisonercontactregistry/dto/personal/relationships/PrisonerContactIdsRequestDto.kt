package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships

import io.swagger.v3.oas.annotations.media.Schema

data class PrisonerContactIdsRequestDto(
  @param:Schema(description = "List of prisoner contact relationship identifiers [NOT contactId]", example = "[123456, 789012]", required = true)
  val prisonerContactIds: List<Long>,
)
