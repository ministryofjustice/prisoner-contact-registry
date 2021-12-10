package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

@Schema(description = "A contact for a prisoner")
class ContactDTO private constructor(
  @Schema(description = "Unique Identifier for this contact (from NOMIS)", example = "123", required = true) val nomisPersonId: String,
  @Schema(description = "Contact first name", example = "123", required = false) val firstName: String?,
  @Schema(description = "Contact middle name", example = "123", required = false) val middleName: String?,
  @Schema(description = "Contact last name", example = "123", required = false) val lastName: String?,
  @Schema(description = "Contact date of birth", example = "123", required = false) val dateOfBirth: LocalDate?,
  @Schema(description = "Code for relationship to Prisoner", example = "123", required = false) val relationshipType: String?,
  @Schema(description = "Description of relationship to Prisoner", example = "123", required = false) val relationshipDescription: String?,
  @Schema(description = "Type of Contact", example = "123", required = false) val contactType: String?,
  @Schema(description = "Description of Contact Type", example = "123", required = false) val contactTypeDescription: String?,
  @Schema(description = "Approved Visitor Flag", required = false) val approvedVisitor: Boolean = false,
  @Schema(description = "Emergency Contact Flag", required = false) val emergencyContact: Boolean = false,
  @Schema(description = "Next of Kin Flag", required = false) val nextOfKin: Boolean = false,
  @Schema(description = "Additional Information", example = "123", required = false) val commentText: String?,
  @Schema(description = "Restriction List", required = false) val restrictions: List<RestrictionDto> = listOf()
) {

  data class Builder(
    // contact ID ???
    var nomisPersonId: String,
    var firstName: String? = null,
    var middleName: String? = null,
    var lastName: String? = null,
    var dateOfBirth: LocalDate? = null,
    var relationshipType: String? = null, // enum
    var relationshipDescription: String? = null,
    var contactType: String? = null, // enum
    var contactTypeDescription: String? = null,
    var approvedVisitor: Boolean = false,
    var emergencyContact: Boolean = false,
    var nextOfKin: Boolean = false,
    var commentText: String? = null,
    var restrictions: List<RestrictionDto> = listOf()
  ) {
    fun nomisPersonId(nomisPersonId: String) = apply { this.nomisPersonId = nomisPersonId }
    fun firstName(firstName: String) = apply { this.firstName = firstName }
    fun middleName(middleName: String) = apply { this.middleName = middleName }
    fun lastName(lastName: String) = apply { this.lastName = lastName }
    fun dateOfBirth(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateOfBirth: LocalDate) = apply { this.dateOfBirth = dateOfBirth }
    fun relationshipType(relationshipType: String) = apply { this.relationshipType = relationshipType }
    fun relationshipDescription(relationshipDescription: String) = apply { this.relationshipDescription = relationshipDescription }
    fun contactType(contactType: String) = apply { this.contactType = contactType }
    fun contactTypeDescription(contactTypeDescription: String) = apply { this.contactTypeDescription = contactTypeDescription }
    fun approvedVisitor(approvedVisitor: Boolean) = apply { this.approvedVisitor = approvedVisitor }
    fun emergencyContact(emergencyContact: Boolean) = apply { this.emergencyContact = emergencyContact }
    fun nextOfKin(nextOfKin: Boolean) = apply { this.nextOfKin = nextOfKin }
    fun commentText(commentText: String) = apply { this.commentText = commentText }
    fun restrictions(restrictions: List<RestrictionDto>) = apply { this.restrictions = restrictions }
    fun build() = ContactDTO(nomisPersonId, firstName, middleName, lastName, dateOfBirth, relationshipType, relationshipDescription, contactType, contactTypeDescription, approvedVisitor, emergencyContact, nextOfKin, commentText, restrictions)
  }
}

// TODO: types
// TODO: required
// TODO: id
