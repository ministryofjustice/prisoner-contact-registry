package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A linked prisoner of contact")
data class ContactLinkedPrisonerDto(
  @param:Schema(description = "Prisoner number (NOMS ID)", example = "A1234BC")
  val prisonerNumber: String,

  @param:Schema(
    description =
    """
    Coded value indicating either a social or official contact (mandatory).
    This is a coded value from the group code CONTACT_TYPE in reference data.
    Known values are (S) Social or (O) official.
    """,
    example = "S",
  )
  val relationshipTypeCode: String,
)
