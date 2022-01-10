package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

@Schema(description = "A contact for a prisoner")
class Restriction private constructor(
  @Schema(description = "Restriction Type Code", example = "123", required = false) val restrictionType: String?,
  @Schema(description = "Description of Restriction Type", example = "123", required = false) val restrictionTypeDescription: String?,
  @Schema(description = "Date from which the restriction applies", example = "2000-10-31", required = false) val startDate: LocalDate?,
  @Schema(description = "Restriction Expiry", example = "2000-10-31", required = false) val expiryDate: LocalDate?,
  @Schema(description = "True if applied globally to the contact or False if applied in the context of a visit", required = false) val globalRestriction: Boolean = false,
  @Schema(description = "Additional Information", example = "This is a comment text", required = false) val comment: String?
) {

  data class Builder(
    var restrictionType: String? = null,
    var restrictionTypeDescription: String? = null,
    var startDate: LocalDate? = null,
    var expiryDate: LocalDate? = null,
    var globalRestriction: Boolean = false,
    var comment: String? = null,
  ) {
    fun restrictionType(restrictionType: String) = apply { this.restrictionType = restrictionType }
    fun restrictionTypeDescription(restrictionTypeDescription: String) = apply { this.restrictionTypeDescription = restrictionTypeDescription }
    fun startDate(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate) = apply { this.startDate = startDate }
    fun expiryDate(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) expiryDate: LocalDate) = apply { this.expiryDate = expiryDate }
    fun globalRestriction(globalRestriction: Boolean) = apply { this.globalRestriction = globalRestriction }
    fun comment(comment: String) = apply { this.comment = comment }
    fun build() = Restriction(restrictionType, restrictionTypeDescription, startDate, expiryDate, globalRestriction, comment)
  }
}
