package uk.gov.justice.digital.hmpps.prisonercontactregistry.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonercontactregistry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.DateRangeDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.HasClosedRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.visit.scheduler.RequestVisitVisitorRestrictionsBodyDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.service.PrisonerContactRegistryServiceV2
import java.time.LocalDate

// TODO: Get sessions date ranges ticket https://dsdmoj.atlassian.net/browse/VB-5723 for more detailed info
//  A. New endpoint (similar to the BAN_DATE_RANGE endpoint structure)
//  B. Takes a prisonerId, visitorIds, existing date range, list of supported restrictions
//  C. Find all visitor restrictions, filter to only keep the ones that are in the list of supported restrictions
//  D. Check if restrictions contains a null expiry (send back original date range), if not, get all date ranges (unique) into a List and return

const val V2_PRISONER_CONTACTS_CONTROLLER_PATH: String = "v2/prisoners/{prisonerId}"
const val V2_PRISONER_GET_SOCIAL_CONTACTS_CONTROLLER_PATH: String = "$V2_PRISONER_CONTACTS_CONTROLLER_PATH/contacts/social"
const val V2_PRISONER_GET_SOCIAL_CONTACTS_APPROVED_CONTROLLER_PATH: String = "$V2_PRISONER_GET_SOCIAL_CONTACTS_CONTROLLER_PATH/approved"
const val V2_PRISONER_GET_SOCIAL_RESTRICTION_CLOSED_CONTROLLER_PATH: String = "$V2_PRISONER_GET_SOCIAL_CONTACTS_APPROVED_CONTROLLER_PATH/restrictions/closed"
const val V2_PRISONER_GET_SOCIAL_RESTRICTION_BANNED_DATE_RANGE_CONTROLLER_PATH: String = "$V2_PRISONER_GET_SOCIAL_CONTACTS_APPROVED_CONTROLLER_PATH/restrictions/banned/dateRange"
const val V2_PRISONER_GET_REQUEST_VISIT_RESTRICTION_DATE_RANGES_CONTROLLER_PATH: String = "$V2_PRISONER_GET_SOCIAL_CONTACTS_APPROVED_CONTROLLER_PATH/restrictions/visit-request/date-ranges"

@RestController
@Validated
@RequestMapping(name = "Contact Resource v2", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerContactControllerV2(
  private val contactService: PrisonerContactRegistryServiceV2,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(V2_PRISONER_GET_SOCIAL_CONTACTS_CONTROLLER_PATH)
  @Operation(
    summary = "Get Prisoners Social Contacts",
    description = "Returns details of a prisoner's social contacts",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner Social Contacts Information Returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to retrieve prisoner's approved social contacts",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to retrieve prisoner's approved social contacts",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPrisonerSocialContacts(
    @Schema(description = "Prisoner Identifier (NOMIS Offender No)", example = "A1234AA", required = true)
    @PathVariable
    prisonerId: String,
    @RequestParam(value = "hasDateOfBirth", required = false)
    @Parameter(description = "Defaults to false. By default when false, returns all contacts with or without a DOB. If true, returns only contacts with a DOB.", example = "false")
    hasDateOfBirth: Boolean? = false,
    @RequestParam(value = "withAddress", required = false)
    @Parameter(description = "by default returns addresses for all contacts, set to false if contact addresses not needed.", example = "false")
    withAddress: Boolean? = true,
  ): List<ContactDto> {
    log.debug("getPrisonerSocialContacts called with params : Prisoner: {}, hasDateOfBirth = {}, withAddress = {}", prisonerId, hasDateOfBirth, withAddress)

    return contactService.getSocialContactList(
      prisonerId = prisonerId,
      withAddress = withAddress ?: true,
      hasDateOfBirth = hasDateOfBirth ?: false,
      approvedContactsOnly = false,
    )
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(V2_PRISONER_GET_SOCIAL_CONTACTS_APPROVED_CONTROLLER_PATH)
  @Operation(
    summary = "Get Prisoners Social Contacts that are approved",
    description = "Returns details of a prisoner's social contacts that have been approved.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner Approved Social Contacts Information Returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to retrieve prisoner's approved social contacts",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to retrieve prisoner's approved social contacts",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPrisonersSocialContactsApproved(
    @Schema(description = "Prisoner Identifier (NOMIS Offender No)", example = "A1234AA", required = true)
    @PathVariable
    prisonerId: String,
    @RequestParam(value = "hasDateOfBirth", required = false)
    @Parameter(description = "Defaults to false. By default when false, returns all contacts with or without a DOB. If true, returns only contacts with a DOB.", example = "false")
    hasDateOfBirth: Boolean? = false,
    @RequestParam(value = "withAddress", required = false)
    @Parameter(description = "by default returns addresses for all contacts, set to false if contact addresses not needed.", example = "false")
    withAddress: Boolean? = true,
  ): List<ContactDto> {
    log.debug("getPrisonersSocialContactsApproved called with params : Prisoner: {}, hasDateOfBirth = {}, withAddress = {}", prisonerId, hasDateOfBirth, withAddress)

    return contactService.getSocialContactList(
      prisonerId = prisonerId,
      withAddress = withAddress ?: true,
      hasDateOfBirth = hasDateOfBirth ?: false,
      approvedContactsOnly = true,
    )
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(V2_PRISONER_GET_SOCIAL_RESTRICTION_BANNED_DATE_RANGE_CONTROLLER_PATH)
  @Operation(
    summary = "Get an updated date range for visitors if a visitor with ban restriction is found, else returns original date",
    description = "Returns an updated date range for visitors if one is found with an active ban restriction. If not, it returns the original date range",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Date range returned (original or adjusted)",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get date range for prisoner visitors",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to Get date range for prisoner visitors",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner, Visitor or Date range not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getUpdatedDateRangeForPrisonerVisitorsIfFoundWithBanRestrictions(
    @Schema(description = "Prisoner Identifier (NOMIS Offender No)", example = "A1234AA", required = true)
    @PathVariable
    prisonerId: String,
    @RequestParam(value = "visitors", required = true)
    @Parameter(description = "Ids of prisoner visitors", example = "9147510, 8431201")
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
      toDate,
    )
    return contactService.getBannedDateRangeForPrisonerContacts(prisonerId, visitorIds, fromDate, toDate)
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(V2_PRISONER_GET_SOCIAL_RESTRICTION_CLOSED_CONTROLLER_PATH)
  @Operation(
    summary = "Get status on any visitors having closed restrictions",
    description = "Returns a boolean true/false for a given list of visitors if any closed restrictions are found",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returned status on visitor closed restrictions successfully",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get status of visitors closed restrictions",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to Get status of visitors closed restrictions",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner or Visitor",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getClosedRestrictionStatusForPrisonerVisitors(
    @Schema(description = "Prisoner Identifier (NOMIS Offender No)", example = "A1234AA", required = true)
    @PathVariable
    prisonerId: String,
    @RequestParam(value = "visitors", required = true)
    @Parameter(description = "Ids of prisoner visitors", example = "9147510, 8431201")
    visitorIds: List<Long>,
  ): HasClosedRestrictionDto {
    log.debug(
      "getHasClosedRestrictionForPrisonerVisitors called with parameters: prisonerId: {}, visitorIds: {}",
      prisonerId,
      visitorIds,
    )
    return contactService.getClosedRestrictionStatusForPrisonerContacts(prisonerId, visitorIds)
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @PostMapping(V2_PRISONER_GET_REQUEST_VISIT_RESTRICTION_DATE_RANGES_CONTROLLER_PATH)
  @Operation(
    summary = "Get an updated date range for visitors if a visitor with ban restriction is found, else returns original date",
    description = "Returns an updated date range for visitors if one is found with an active ban restriction. If not, it returns the original date range",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Date range returned (original or adjusted)",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get date range for prisoner visitors",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to Get date range for prisoner visitors",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner, Visitor or Date range not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getDateRangesForVisitorRestrictionsWhichEffectRequestVisits(
    @RequestBody @Valid
    requestVisitVisitorRestrictionsDto: RequestVisitVisitorRestrictionsBodyDto,
  ): List<DateRangeDto> = contactService.getDateRangesForVisitorRestrictionsWhichEffectRequestVisits(requestVisitVisitorRestrictionsDto)
}
