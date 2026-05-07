package uk.gov.justice.digital.hmpps.prisonercontactregistry.mappers

import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.AddressDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.PrisonerContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsPrisonerContactDto

fun PersonalRelationshipsPrisonerContactDto.toPrisonerContactDto(
  restrictions: List<RestrictionDto>,
): PrisonerContactDto = PrisonerContactDto(
  personId = contactId,
  firstName = firstName.toNormalCase().orEmpty(),
  middleName = middleNames.toNormalCase(),
  lastName = lastName.toNormalCase().orEmpty(),
  dateOfBirth = dateOfBirth,
  relationshipCode = relationshipToPrisonerCode,
  relationshipDescription = relationshipToPrisonerDescription,
  contactType = relationshipTypeCode,
  contactTypeDescription = relationshipTypeDescription,
  approvedVisitor = isApprovedVisitor,
  emergencyContact = isEmergencyContact,
  nextOfKin = isNextOfKin,
  restrictions = restrictions,
  address = AddressDto(this),
  commentText = comments,
)

private fun String?.toNormalCase(): String? = this
  ?.lowercase()
  ?.split(" ")
  ?.joinToString(" ") { word ->
    word.replaceFirstChar { it.uppercase() }
  }
