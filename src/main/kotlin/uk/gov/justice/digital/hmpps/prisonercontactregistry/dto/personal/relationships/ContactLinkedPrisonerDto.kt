package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A linked prisoner of contact")
data class ContactLinkedPrisonerDto(
  @param:Schema(description = "Prisoner number (NOMS ID)", example = "A1234BC")
  val prisonerNumber: String,
)
