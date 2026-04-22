package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsContactDto
import java.time.LocalDate

@Schema(description = "A contact (no prisoner relationship)")
data class ContactDto(
  @param:Schema(description = "Identifier for this contact", example = "5871791")
  val contactId: Long? = null,
  @param:Schema(description = "First name", example = "John", required = true)
  val firstName: String,
  @param:Schema(description = "Middle name", example = "Mark", required = false)
  val middleName: String? = null,
  @param:Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,
  @param:Schema(description = "Date of birth", example = "1980-01-28", required = false)
  val dateOfBirth: LocalDate? = null,
) {
  constructor(personalRelationshipsContact: PersonalRelationshipsContactDto) : this(
    contactId = personalRelationshipsContact.id,
    firstName = personalRelationshipsContact.firstName.toNormalCase()!!,
    middleName = personalRelationshipsContact.middleNames.toNormalCase(),
    lastName = personalRelationshipsContact.lastName.toNormalCase()!!,
    dateOfBirth = personalRelationshipsContact.dateOfBirth,
  )
}

private fun String?.toNormalCase(): String? = this
  ?.lowercase()
  ?.split(" ")
  ?.joinToString(" ") { word ->
    word.replaceFirstChar { it.uppercase() }
  }
