package uk.gov.justice.digital.hmpps.prisonercontactregistry.enum

enum class RestrictionType(private val restriction: String) {
  BANNED("BAN"),
  CLOSED("CLOSED"),
  ;

  override fun toString(): String {
    return restriction
  }
}
