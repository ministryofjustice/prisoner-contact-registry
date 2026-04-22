package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PersonalRelationshipsContactDto(

  @param:Schema(description = "The id of the contact", example = "123456")
  val id: Long,

  @param:Schema(description = "The first name of the contact", example = "John")
  val firstName: String,

  @param:Schema(description = "The middle name of the contact, if any", example = "William", nullable = true)
  val middleNames: String? = null,

  @param:Schema(description = "The last name of the contact", example = "Doe")
  val lastName: String,

  @param:Schema(description = "The date of birth of the contact, if known", example = "1980-01-01", nullable = true)
  val dateOfBirth: LocalDate? = null,
)
