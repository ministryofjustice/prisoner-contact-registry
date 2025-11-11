package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "A date range")
data class DateRangeDto(

  @param:Schema(description = "The start of the date range")
  var fromDate: LocalDate,

  @param:Schema(description = "The end of the date range")
  var toDate: LocalDate,
)
