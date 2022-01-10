package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "An Offender's address usage")
class AddressUsage private constructor(
  @Schema(description = "The address usages", example = "HDC", required = false) val addressUsage: String?,
  @Schema(description = "The address usages description", example = "HDC Address", required = false) val addressUsageDescription: String?,
  @Schema(description = "Active Flag", example = "true", required = false) val activeFlag: Boolean?
) {
  data class Builder(
    var addressUsage: String? = null,
    var addressUsageDescription: String? = null,
    var activeFlag: Boolean? = null,
  ) {
    fun addressUsage(addressUsage: String) = apply { this.addressUsage = addressUsage }
    fun addressUsageDescription(addressUsageDescription: String) = apply { this.addressUsageDescription = addressUsageDescription }
    fun activeFlag(activeFlag: Boolean) = apply { this.activeFlag = activeFlag }
    fun build() = AddressUsage(addressUsage, addressUsageDescription, activeFlag)
  }
}
