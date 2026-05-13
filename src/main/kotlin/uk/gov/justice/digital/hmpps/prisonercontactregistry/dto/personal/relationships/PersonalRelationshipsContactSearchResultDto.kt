package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.AddressDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactWithOptionalPrisonerRelationshipDto
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "The details of a contact as an individual with a list of any prisoner relationships they have")
data class PersonalRelationshipsContactSearchResultDto(

  @param:Schema(description = "The id of the contact", example = "123456")
  val id: Long,

  @param:Schema(description = "The last name of the contact", example = "Doe")
  val lastName: String,

  @param:Schema(description = "The first name of the contact", example = "John")
  val firstName: String,

  @param:Schema(description = "The middle name of the contact, if any", example = "William", nullable = true)
  val middleNames: String? = null,

  @param:Schema(description = "The date of birth of the contact, if known", example = "1980-01-01", nullable = true)
  val dateOfBirth: LocalDate? = null,

  @param:Schema(description = "The date the contact deceased, if known", example = "1980-01-01", nullable = true)
  val deceasedDate: LocalDate? = null,

  @param:Schema(description = "The id of the user who created the contact", example = "JD000001")
  val createdBy: String? = null,

  @param:Schema(description = "The timestamp of when the contact was created", example = "2024-01-01T00:00:00Z")
  val createdTime: LocalDateTime? = null,

  @param:Schema(description = "The flat of the contact address, if known", example = "01", nullable = true)
  val flat: String? = null,

  @param:Schema(description = "The property of the contact address, if known", example = "01", nullable = true)
  val property: String? = null,

  @param:Schema(description = "The street of the contact address, if known", example = "Bluebell Crescent", nullable = true)
  val street: String? = null,

  @param:Schema(description = "The area of the contact address, if known", example = "Birmingham", nullable = true)
  val area: String? = null,

  @param:Schema(description = "The city code of the contact address, if known", example = "25343", nullable = true)
  val cityCode: String? = null,

  @param:Schema(description = "The description of city code, if known", example = "Sheffield", nullable = true)
  val cityDescription: String? = null,

  @param:Schema(description = "The county code of the contact address, if known", example = "S.YORKSHIRE", nullable = true)
  val countyCode: String? = null,

  @param:Schema(description = "The description of county code, if known", example = "South Yorkshire", nullable = true)
  val countyDescription: String? = null,

  @param:Schema(description = "The postcode of the contact address, if known", example = "B42 2QJ", nullable = true)
  val postcode: String? = null,

  @param:Schema(description = "The country code of the contact address, if known", example = "ENG", nullable = true)
  val countryCode: String? = null,

  @param:Schema(description = "The description of country code, if known", example = "England", nullable = true)
  val countryDescription: String? = null,

  @param:Schema(
    description = "If true this address should be considered for sending mail to",
    nullable = true,
    example = "true",
  )
  val mailAddress: Boolean?,

  @param:Schema(
    description = "The date from which this address can be considered active",
    example = "2022-10-01",
    nullable = true,
  )
  val startDate: LocalDate? = null,

  @param:Schema(
    description = "The date after which this address should be considered inactive",
    example = "2023-10-02",
    nullable = true,
  )
  val endDate: LocalDate? = null,

  @param:Schema(
    description = "A flag to indicate that this address is effectively no fixed address",
    example = "false",
    nullable = true,
  )
  val noFixedAddress: Boolean? = false,

  @param:Schema(
    description = "Any additional information or comments about the address",
    example = "Some additional information",
    nullable = true,
  )
  val comments: String?,

  @param:Schema(
    description = "A list of existing relationships to a prisoner if a check against the prisoner number was requested. " +
      "Empty if there are no existing relationships or null if it was not requested.",
    nullable = true,
    required = false,
  )
  val existingRelationships: List<ExistingRelationshipToPrisonerDto>? = null,
)

fun PersonalRelationshipsContactSearchResultDto.toContactWithOptionalPrisonerRelationshipDto(): List<ContactWithOptionalPrisonerRelationshipDto> {
  val address = if (
    listOf(
      flat,
      property,
      street,
      area,
      cityDescription,
      postcode,
      countyDescription,
      countryDescription,
      comments,
    ).any { it != null } ||
    mailAddress == true ||
    noFixedAddress == true
  ) {
    AddressDto(
      flat = flat,
      premise = property,
      street = street,
      locality = area,
      town = cityDescription,
      postalCode = postcode,
      county = countyDescription,
      country = countryDescription,
      comment = comments,
      primary = mailAddress ?: false,
      noFixedAddress = noFixedAddress ?: false,
    )
  } else {
    null
  }

  return if (existingRelationships.isNullOrEmpty()) {
    listOf(
      ContactWithOptionalPrisonerRelationshipDto(
        contactId = id,
        prisonerContactId = null,
        firstName = firstName.toNormalCase().orEmpty(),
        middleName = middleNames.toNormalCase(),
        lastName = lastName.toNormalCase().orEmpty(),
        dateOfBirth = dateOfBirth,
        relationshipCode = null,
        relationshipDescription = null,
        contactType = null,
        contactTypeDescription = null,
        restrictions = emptyList(), // Restrictions are handled after retrieval of contact information, hence setting to emptyList() here.
        address = address,
        approvedVisitor = null,
        emergencyContact = null,
        nextOfKin = null,
        comments = null,
      ),
    )
  } else {
    existingRelationships.map { relationship ->
      ContactWithOptionalPrisonerRelationshipDto(
        contactId = id,
        prisonerContactId = relationship.prisonerContactId,
        firstName = firstName.toNormalCase().orEmpty(),
        middleName = middleNames.toNormalCase(),
        lastName = lastName.toNormalCase().orEmpty(),
        dateOfBirth = dateOfBirth,
        relationshipCode = relationship.relationshipToPrisonerCode,
        relationshipDescription = relationship.relationshipToPrisonerDescription,
        contactType = relationship.relationshipTypeCode,
        contactTypeDescription = relationship.relationshipTypeDescription,
        restrictions = emptyList(), // Restrictions are handled after retrieval of contact information, hence setting to emptyList() here.
        address = address,
        approvedVisitor = relationship.isApprovedVisitor,
        emergencyContact = relationship.isEmergencyContact,
        nextOfKin = relationship.isNextOfKin,
        comments = relationship.comments,
      )
    }
  }
}

private fun String?.toNormalCase(): String? = this
  ?.lowercase()
  ?.split(" ")
  ?.joinToString(" ") { word ->
    word.replaceFirstChar { it.uppercase() }
  }
