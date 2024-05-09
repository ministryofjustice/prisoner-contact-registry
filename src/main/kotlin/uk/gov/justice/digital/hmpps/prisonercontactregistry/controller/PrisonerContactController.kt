package uk.gov.justice.digital.hmpps.prisonercontactregistry.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonercontactregistry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.DateRangeDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.service.PrisonerContactRegistryService
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(name = "Contact Resource", path = ["/prisoners"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerContactController(
  private val contactService: PrisonerContactRegistryService,
  private val prisonerContactRegistryService: PrisonerContactRegistryService,
) {

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping("{prisonerId}/contacts")
  @Operation(
    summary = "Get Prisoner Contact",
    description = "Returns details of a prisoner contacts",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner Contacts Information Returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get Prisoner Contacts for Prisoner Identifier",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions retrieve a Prisoner Contacts",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPrisonerContact(
    @Schema(description = "Prisoner Identifier (NOMIS Offender No)", example = "A1234AA", required = true)
    @PathVariable
    prisonerId: String,
    @RequestParam(value = "type", required = false)
    @Parameter(description = "Query by Type (NOMIS Contact Type)", example = "S")
    contactType: String?,
    @RequestParam(value = "id", required = false)
    @Parameter(description = "Query by Person Identifier (NOMIS Person ID)", example = "9147510")
    personId: Long?,
    @RequestParam(value = "withAddress", required = false)
    @Parameter(description = "by default returns addresses for all contacts, set to false if contact addresses not needed.", example = "false")
    withAddress: Boolean? = true,
  ): List<ContactDto> {
    log.debug("Prisoner: $prisonerId, Type: $contactType, Person: $personId, withAddress = $withAddress")
    return orderByLastNameAndThenFirstName(contactService.getContactList(prisonerId, contactType, personId, withAddress))
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping("{prisonerId}/approved/social/contacts/restrictions/banned/dateRange")
  @Operation(
    summary = "Get banned date range for prisoner contacts",
    description = "Returns a banned date range for given list of prisoner contacts",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Banned date range returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get banned date range for prisoner contacts",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to Get banned date range for prisoner contacts",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Date range not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getBannedDateRangeForPrisonerContacts(
    @Schema(description = "Prisoner Identifier (NOMIS Offender No)", example = "A1234AA", required = true)
    @PathVariable
    prisonerId: String,
    @RequestParam(value = "visitors", required = true)
    @Parameter(description = "Visitor Ids of prisoner contacts", example = "9147510, 8431201")
    visitorIds: List<Long>,
    @RequestParam(value = "fromDate", required = true)
    @Parameter(description = "Start date range", example = "2024-03-15")
    fromDate: LocalDate,
    @RequestParam(value = "toDate", required = true)
    @Parameter(description = "To date range", example = "2024-03-31")
    toDate: LocalDate,
  ): DateRangeDto {
    log.debug(
      "getBannedDateRangeForPrisonerContacts called with parameters: prisonerId: {}, visitorIds: {}, fromDate: {}, toDate: {}",
      prisonerId,
      visitorIds,
      fromDate,
      toDate
    )
    return prisonerContactRegistryService.getBannedDateRangeForPrisonerContacts(prisonerId, visitorIds, fromDate, toDate)
  }

  private fun orderByLastNameAndThenFirstName(contactList: List<ContactDto>): List<ContactDto> {
    return contactList.sortedWith(compareBy({ it.lastName }, { it.firstName }))
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
