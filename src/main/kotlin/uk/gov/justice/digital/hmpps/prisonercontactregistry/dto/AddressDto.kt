package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsPrisonerContactDto

@Schema(description = "An address")
class AddressDto(
  @param:Schema(description = "Flat", example = "3B", required = false)
  val flat: String? = null,
  @param:Schema(description = "Premise", example = "Liverpool Prison", required = false)
  val premise: String? = null,
  @param:Schema(description = "Street", example = "Slinn Street", required = false)
  val street: String? = null,
  @param:Schema(description = "Locality", example = "Brincliffe", required = false)
  val locality: String? = null,
  @param:Schema(description = "Town/City", example = "Liverpool", required = false)
  val town: String? = null,
  @param:Schema(description = "Postal Code", example = "LI1 5TH", required = false)
  val postalCode: String? = null,
  @param:Schema(description = "County", example = "HEREFORD", required = false)
  val county: String? = null,
  @param:Schema(description = "Country", example = "ENG", required = false)
  val country: String? = null,
  @param:Schema(description = "Additional Information", example = "This is a comment text", required = false)
  val comment: String? = null,
  @param:Schema(description = "Primary Address", example = "Y", required = true)
  val primary: Boolean,
  @param:Schema(description = "No Fixed Address", example = "N", required = true)
  val noFixedAddress: Boolean,
) {
  constructor(personalRelationshipsPrisonerContactDto: PersonalRelationshipsPrisonerContactDto) : this(
    flat = personalRelationshipsPrisonerContactDto.flat,
    premise = personalRelationshipsPrisonerContactDto.property,
    street = personalRelationshipsPrisonerContactDto.street,
    locality = personalRelationshipsPrisonerContactDto.area,
    town = personalRelationshipsPrisonerContactDto.cityDescription,
    postalCode = personalRelationshipsPrisonerContactDto.postcode,
    county = personalRelationshipsPrisonerContactDto.countyDescription,
    country = personalRelationshipsPrisonerContactDto.countryDescription,
    comment = personalRelationshipsPrisonerContactDto.comments,
    primary = personalRelationshipsPrisonerContactDto.primaryAddress ?: false,
    noFixedAddress = personalRelationshipsPrisonerContactDto.noFixedAddress ?: false,
  )
}
