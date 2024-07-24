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
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.HasClosedRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.VisitorActiveRestrictionsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.service.PrisonerContactRegistryService
import java.time.LocalDate

const val PRISON_CONTACTS_CONTROLLER_PATH: String = "/prisoners/{prisonerId}"
const val PRISON_GET_ALL_CONTACTS_CONTROLLER_PATH: String = "$PRISON_CONTACTS_CONTROLLER_PATH/contacts"
const val PRISON_GET_SOCIAL_CONTACTS_CONTROLLER_PATH: String = "$PRISON_CONTACTS_CONTROLLER_PATH/contacts/social"

const val PRISON_GET_ACTIVE_RESTRICTIONS_CONTROLLER_PATH: String = "/prisoners/{prisonerId}/contacts/social/approved/{visitorId}/restrictions/active"

// TODO: This endpoint is deprecated now. Remove in future.
const val PRISON_GET_APPROVED_SOCIAL_CONTACTS_CONTROLLER_PATH: String = "$PRISON_CONTACTS_CONTROLLER_PATH/approved/social/contacts"

// TODO: These endpoints need updating to be correct structure: "/prisoners/{prisonerId}/contacts/social/approved/..."
const val PRISON_GET_BANNED_DATE_RANGE_CONTROLLER_PATH: String = "$PRISON_GET_APPROVED_SOCIAL_CONTACTS_CONTROLLER_PATH/restrictions/banned/dateRange"
const val PRISON_GET_CLOSED_RESTRICTIONS_CONTROLLER_PATH: String = "$PRISON_GET_APPROVED_SOCIAL_CONTACTS_CONTROLLER_PATH/restrictions/closed"

@RestController
@Validated
@RequestMapping(name = "Contact Resource", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerContactController(
  private val contactService: PrisonerContactRegistryService,
  private val prisonerContactRegistryService: PrisonerContactRegistryService,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(PRISON_GET_ALL_CONTACTS_CONTROLLER_PATH)
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
    @Parameter(
      description = "by default returns addresses for all contacts, set to false if contact addresses not needed.",
      example = "false",
    )
    withAddress: Boolean? = true,
  ): List<ContactDto> {
    log.debug("Prisoner: $prisonerId, Type: $contactType, Person: $personId, withAddress = $withAddress")
    return contactService.getContactList(prisonerId, contactType, personId, withAddress, approvedVisitorsOnly = false)
      .sortedWith(getDefaultSortOrder())
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(PRISON_GET_SOCIAL_CONTACTS_CONTROLLER_PATH)
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
    @RequestParam(value = "id", required = false)
    @Parameter(description = "Query by Person Identifier (NOMIS Person ID)", example = "9147510")
    personId: Long?,
    @RequestParam(value = "hasDateOfBirth", required = false)
    @Parameter(description = "Defaults to null. By default (or when false), returns all contacts with or without a DOB. If true, returns only contacts with a DOB.", example = "false")
    hasDateOfBirth: Boolean? = null,
    @RequestParam(value = "notBannedBeforeDate", required = false)
    @Parameter(description = "Get only visitors that have a ban date that expires before this date. Gets all visitors irrespective of BANs if not passed.", example = "2024-12-31", required = false)
    notBannedBeforeDate: LocalDate? = null,
    @RequestParam(value = "withAddress", required = false)
    @Parameter(description = "by default returns addresses for all contacts, set to false if contact addresses not needed.", example = "false")
    withAddress: Boolean? = true,
    @Parameter(description = "by default set to true and will return only approved social contacts. If false, returns all social contacts", example = "false")
    approvedVisitorsOnly: Boolean? = true,
  ): List<ContactDto> {
    log.debug("getPrisonerSocialContacts called with params : Prisoner: {}, id : {}, hasDateOfBirth = {}, notBannedBeforeDate = {}, withAddress = {}, approvedVisitorsOnly = {}", prisonerId, personId, hasDateOfBirth, notBannedBeforeDate, withAddress, approvedVisitorsOnly)

    return contactService.getSocialContactList(
      prisonerId = prisonerId,
      personId = personId,
      withAddress = withAddress ?: true,
      hasDateOfBirth = hasDateOfBirth,
      notBannedBeforeDate = notBannedBeforeDate,
      approvedVisitorsOnly = approvedVisitorsOnly ?: true,
    ).sortedWith(getDefaultSortOrder())
  }

  @Deprecated("This has been replaced by PRISON_GET_SOCIAL_CONTACTS_CONTROLLER_PATH endpoint")
  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(PRISON_GET_APPROVED_SOCIAL_CONTACTS_CONTROLLER_PATH)
  @Operation(
    summary = "Get Prisoners approved Social Contacts",
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
  fun getPrisonersApprovedSocialContacts(
    @Schema(description = "Prisoner Identifier (NOMIS Offender No)", example = "A1234AA", required = true)
    @PathVariable
    prisonerId: String,
    @RequestParam(value = "id", required = false)
    @Parameter(description = "Query by Person Identifier (NOMIS Person ID)", example = "9147510")
    personId: Long?,
    @RequestParam(value = "hasDateOfBirth", required = false)
    @Parameter(description = "Defaults to null. By default (or when false), returns all contacts with or without a DOB. If true, returns only contacts with a DOB.", example = "false")
    hasDateOfBirth: Boolean? = null,
    @RequestParam(value = "notBannedBeforeDate", required = false)
    @Parameter(description = "Get only visitors that have a ban date that expires before this date. Gets all visitors irrespective of BANs if not passed.", example = "2024-12-31", required = false)
    notBannedBeforeDate: LocalDate? = null,
    @RequestParam(value = "withAddress", required = false)
    @Parameter(description = "by default returns addresses for all contacts, set to false if contact addresses not needed.", example = "false")
    withAddress: Boolean? = true,
  ): List<ContactDto> {
    log.debug("getPrisonersApprovedSocialContacts called with params : Prisoner: {}, id : {}, hasDateOfBirth = {}, notBannedBeforeDate = {}, withAddress = {}", prisonerId, personId, hasDateOfBirth, notBannedBeforeDate, withAddress)

    return contactService.getSocialContactList(
      prisonerId = prisonerId,
      personId = personId,
      withAddress = withAddress ?: true,
      hasDateOfBirth = hasDateOfBirth,
      notBannedBeforeDate = notBannedBeforeDate,
      approvedVisitorsOnly = true,
    ).sortedWith(getDefaultSortOrder())
  }

  private final fun getDefaultSortOrder(): Comparator<ContactDto> {
    return compareBy({ it.lastName }, { it.firstName })
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(PRISON_GET_BANNED_DATE_RANGE_CONTROLLER_PATH)
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
    return prisonerContactRegistryService.getBannedDateRangeForPrisonerContacts(prisonerId, visitorIds, fromDate, toDate)
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(PRISON_GET_CLOSED_RESTRICTIONS_CONTROLLER_PATH)
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
        description = "Prisoner or Visitor not found",
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
    return prisonerContactRegistryService.getClosedRestrictionStatusForPrisonerContacts(prisonerId, visitorIds)
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(PRISON_GET_ACTIVE_RESTRICTIONS_CONTROLLER_PATH)
  @Operation(
    summary = "Get active restrictions of given visitor",
    description = "Returns a list of active restrictions for a single visitor",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returned all active restrictions for visitor",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get active restrictions of visitor",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to Get active restrictions of visitor",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner or Visitor not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getActiveRestrictionsForPrisonerVisitor(
    @Schema(description = "Prisoner Identifier (NOMIS Offender No)", example = "A1234AA", required = true)
    @PathVariable
    prisonerId: String,
    @Schema(description = "Id of prisoner visitor", example = "9147510", required = true)
    @PathVariable
    visitorId: Long,
  ): VisitorActiveRestrictionsDto {
    log.debug(
      "getActiveRestrictionsForPrisonerVisitor called with parameters: prisonerId: {}, visitorId: {}",
      prisonerId,
      visitorId,
    )
    return prisonerContactRegistryService.getActiveRestrictionsForPrisonerContact(prisonerId, visitorId)
  }
}
