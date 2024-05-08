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
import uk.gov.justice.digital.hmpps.prisonercontactregistry.service.PrisonerContactRegistryService
import java.time.LocalDate

const val PRISON_CONTACTS_CONTROLLER_PATH: String = "/prisoners/{prisonerId}"
const val PRISON_GET_ALL_CONTACTS_CONTROLLER_PATH: String = "$PRISON_CONTACTS_CONTROLLER_PATH/contacts"
const val PRISON_GET_APRROVED_SOCIAL_CONTACTS_CONTROLLER_PATH: String = "$PRISON_CONTACTS_CONTROLLER_PATH/approved/social/contacts"

@RestController
@Validated
@RequestMapping(name = "Contact Resource", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerContactController(
  private val contactService: PrisonerContactRegistryService,
) {

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
    @Parameter(description = "by default returns addresses for all contacts, set to false if contact addresses not needed.", example = "false")
    withAddress: Boolean? = true,
  ): List<ContactDto> {
    log.debug("Prisoner: $prisonerId, Type: $contactType, Person: $personId, withAddress = $withAddress")
    return contactService.getContactList(prisonerId, contactType, personId, withAddress)
      .sortedWith(getDefaultSortOrder())
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(PRISON_GET_APRROVED_SOCIAL_CONTACTS_CONTROLLER_PATH)
  @Operation(
    summary = "Get Prisoners approved Social Contacts",
    description = "Returns details of a prisoner's social contacts that have been approved.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner Social Contacts Information Returned",
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

    return contactService.getApprovedSocialContactList(
      prisonerId = prisonerId,
      personId = personId,
      withAddress = withAddress ?: true,
      hasDateOfBirth = hasDateOfBirth,
      notBannedBeforeDate = notBannedBeforeDate,
    ).sortedWith(getDefaultSortOrder())
  }

  private final fun getDefaultSortOrder(): Comparator<ContactDto> {
    return compareBy({ it.lastName }, { it.firstName })
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
