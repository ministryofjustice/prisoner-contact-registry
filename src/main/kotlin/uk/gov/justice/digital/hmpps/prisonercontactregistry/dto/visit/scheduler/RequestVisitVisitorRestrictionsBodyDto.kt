package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.DateRangeDto

@Schema(description = "A contact for a prisoner")
data class RequestVisitVisitorRestrictionsBodyDto(
  @param:Schema(description = "Prisoner Id of prisoner who the visit is for", required = true)
  @field:NotBlank
  val prisonerId: String,

  @param:Schema(description = "List of all visitors attending the visit", required = true)
  @field:NotEmpty
  val visitorIds: List<String>,

  @param:Schema(description = "A list of restriction codes to search for when finding visitor restrictions", required = true)
  val supportedVisitorRestrictionsCodesForRequestVisits: List<String>,

  @param:Schema(description = "The current visit booking window date range (used to limit restriction search)", required = true)
  val currentDateRange: DateRangeDto,
)
