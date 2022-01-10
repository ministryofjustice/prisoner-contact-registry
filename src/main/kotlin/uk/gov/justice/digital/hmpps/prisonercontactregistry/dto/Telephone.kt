package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Telephone Details")
class Telephone private constructor(
  @Schema(description = "Telephone number", example = "0114 2345678", required = true) val number: String,
  @Schema(description = "Telephone type", example = "TEL", required = true) val type: String,
  @Schema(description = "Telephone extension number", example = "123", required = false) val ext: String?
) {

  data class Builder(
    var number: String,
    var type: String,
    var ext: String? = null,
  ) {
    fun number(number: String) = apply { this.number = number }
    fun type(type: String) = apply { this.type = type }
    fun ext(ext: String) = apply { this.ext = ext }
    fun build() = Telephone(number, type, ext)
  }
}
