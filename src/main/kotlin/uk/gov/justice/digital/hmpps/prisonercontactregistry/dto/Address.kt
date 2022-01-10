package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

@Schema(description = "An address")
class Address private constructor(
  @Schema(description = "Address Type", example = "BUS", required = false) val addressType: String?,
  @Schema(description = "Flat", example = "3B", required = false) val flat: String?,
  @Schema(description = "Premise", example = "Liverpool Prison", required = false) val premise: String?,
  @Schema(description = "Street", example = "Slinn Street", required = false) val street: String?,
  @Schema(description = "Locality", example = "Brincliffe", required = false) val locality: String?,
  @Schema(description = "Town/City", example = "Liverpool", required = false) val town: String?,
  @Schema(description = "Postal Code", example = "LI1 5TH", required = false) val postalCode: String?,
  @Schema(description = "County", example = "HEREFORD", required = false) val county: String?,
  @Schema(description = "Country", example = "ENG", required = false) val country: String?,
  @Schema(description = "Additional Information", example = "This is a comment text", required = false) val comment: String?,
  @Schema(description = "Primary Address", example = "Y", required = true) val primary: Boolean,
  @Schema(description = "No Fixed Address", example = "N", required = true) val noFixedAddress: Boolean,
  @Schema(description = "Date Added", example = "2000-10-31", required = false) val startDate: LocalDate?,
  @Schema(description = "Date ended", example = "2000-10-31", required = false) val endDate: LocalDate?,
  @Schema(description = "The phone number associated with the address", required = false) val phones: List<Telephone> = listOf(),
  @Schema(description = "The address usages/types", required = false) val addressUsages: List<AddressUsage> = listOf()
) {

  // The person has a list of addresses
  data class Builder(
    var addressType: String? = null,
    var flat: String? = null,
    var premise: String? = null,
    var street: String? = null,
    var locality: String? = null,
    var town: String? = null,
    var postalCode: String? = null,
    var county: String? = null,
    var country: String? = null,
    var comment: String? = null,
    var primary: Boolean,
    var noFixedAddress: Boolean,
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,
    var phones: List<Telephone> = listOf(),
    var addressUsages: List<AddressUsage> = listOf(),
  ) {
    fun addressType(addressType: String) = apply { this.addressType = addressType }
    fun flat(flat: String) = apply { this.flat = flat }
    fun premise(premise: String) = apply { this.premise = premise }
    fun street(street: String) = apply { this.street = street }
    fun locality(locality: String) = apply { this.locality = locality }
    fun town(town: String) = apply { this.town = town }
    fun postalCode(postalCode: String) = apply { this.postalCode = postalCode }
    fun county(county: String) = apply { this.county = county }
    fun country(country: String) = apply { this.country = country }
    fun comment(comment: String) = apply { this.comment = comment }
    fun primary(primary: Boolean) = apply { this.primary = primary }
    fun noFixedAddress(noFixedAddress: Boolean) = apply { this.noFixedAddress = noFixedAddress }
    fun startDate(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate) = apply { this.startDate = startDate }
    fun endDate(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate) = apply { this.endDate = endDate }
    fun phones(phones: List<Telephone>) = apply { this.phones = phones }
    fun addressUsages(addressUsages: List<AddressUsage>) = apply { this.addressUsages = addressUsages }
    fun build() = Address(addressType, flat, premise, street, locality, town, postalCode, county, country, comment, primary, noFixedAddress, startDate, endDate, phones, addressUsages)
  }
}
