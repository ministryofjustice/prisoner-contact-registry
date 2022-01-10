package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

@Schema(description = "A contact for a prisoner")
class Contact private constructor(
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791", required = true) val personId: Long,
  @Schema(description = "First name", example = "John", required = true) val firstName: String?,
  @Schema(description = "Middle name", example = "Mark", required = false) val middleName: String?,
  @Schema(description = "Last name", example = "Smith", required = true) val lastName: String?,
  @Schema(description = "Date of birth", example = "1980-01-28", required = false) val dateOfBirth: LocalDate?,
  @Schema(description = "Code for relationship to Prisoner", example = "RO", required = true) val relationshipCode: String?,
  @Schema(description = "Description of relationship to Prisoner", example = "Responsible Officer", required = false) val relationshipDescription: String?,
  @Schema(description = "Type of Contact", example = "O", required = true) val contactType: String?,
  @Schema(description = "Description of Contact Type", example = "Official", required = false) val contactTypeDescription: String?,
  @Schema(description = "Approved Visitor Flag", required = true) val approvedVisitor: Boolean = false,
  @Schema(description = "Emergency Contact Flag", required = true) val emergencyContact: Boolean = false,
  @Schema(description = "Next of Kin Flag", required = true) val nextOfKin: Boolean = false,
  // @Schema(description = "Offender Booking Id for this contact", example = "2468081", required = true) val bookingId: Long,
  // @Schema(description = "List of emails associated with the contact", required = false) val emails: List<ContactEmail> = listOf(),
  @Schema(description = "List of restrictions associated with the contact", required = false) val restrictions: List<Restriction> = listOf(),
  @Schema(description = "List of addresses associated with the contact", required = false) var addresses: List<Address> = listOf(),
  @Schema(description = "Additional Information", example = "This is a comment text", required = false) val commentText: String?
) {
  // The offender has a list of contacts (nomis personID) who are associated with a prisoner via bookings
  data class Builder(
    var personId: Long,
    var firstName: String? = null,
    var middleName: String? = null,
    var lastName: String? = null,
    var dateOfBirth: LocalDate? = null,
    var relationshipCode: String? = null, // enum
    var relationshipDescription: String? = null,
    var contactType: String? = null, // enum
    var contactTypeDescription: String? = null,
    var approvedVisitor: Boolean = false,
    var emergencyContact: Boolean = false,
    var nextOfKin: Boolean = false,
    // var bookingId: Long,
    // var emails: List<ContactEmail> = listOf(),
    var restrictions: List<Restriction> = listOf(),
    var addresses: List<Address> = listOf(),
    var commentText: String? = null
  ) {
    fun personId(personId: Long) = apply { this.personId = personId }
    fun firstName(firstName: String) = apply { this.firstName = firstName }
    fun middleName(middleName: String) = apply { this.middleName = middleName }
    fun lastName(lastName: String) = apply { this.lastName = lastName }
    fun dateOfBirth(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateOfBirth: LocalDate) = apply { this.dateOfBirth = dateOfBirth }
    fun relationshipCode(relationshipCode: String) = apply { this.relationshipCode = relationshipCode }
    fun relationshipDescription(relationshipDescription: String) = apply { this.relationshipDescription = relationshipDescription }
    fun contactType(contactType: String) = apply { this.contactType = contactType }
    fun contactTypeDescription(contactTypeDescription: String) = apply { this.contactTypeDescription = contactTypeDescription }
    fun approvedVisitor(approvedVisitor: Boolean) = apply { this.approvedVisitor = approvedVisitor }
    fun emergencyContact(emergencyContact: Boolean) = apply { this.emergencyContact = emergencyContact }
    fun nextOfKin(nextOfKin: Boolean) = apply { this.nextOfKin = nextOfKin }
    // fun bookingId(bookingId: Long) = apply { this.bookingId = bookingId }
    // fun emails(emails: List<ContactEmail>) = apply { this.emails = emails }
    fun restrictions(restrictions: List<Restriction>) = apply { this.restrictions = restrictions }
    fun addresses(addresses: List<Address>) = apply { this.addresses = addresses }
    fun commentText(commentText: String) = apply { this.commentText = commentText }
    fun build() = Contact(personId, firstName, middleName, lastName, dateOfBirth, relationshipCode, relationshipDescription, contactType, contactTypeDescription, approvedVisitor, emergencyContact, nextOfKin, restrictions, addresses, commentText)
  }
}
