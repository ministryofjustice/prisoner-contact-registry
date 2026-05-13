package uk.gov.justice.digital.hmpps.prisonercontactregistry.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
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
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactWithOptionalPrisonerRelationshipDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.ContactLinkedPrisonerDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.service.ContactsService

const val CONTACTS_CONTROLLER_PATH: String = "v2/contacts"
const val SINGLE_CONTACT_PATH: String = "$CONTACTS_CONTROLLER_PATH/{contactId}"
const val GET_CONTACT_LINKED_PRISONERS_CONTROLLER_PATH: String = "$SINGLE_CONTACT_PATH/linked-social-prisoners"
const val GET_CONTACT_GLOBAL_RESTRICTIONS_CONTROLLER_PATH: String = "$SINGLE_CONTACT_PATH/restrictions/global"

const val CONTACT_SEARCH_PATH: String = "$CONTACTS_CONTROLLER_PATH/search"

@RestController
@Validated
@RequestMapping(name = "Contact Resource v2", produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactsController(
  private val contactsService: ContactsService,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(SINGLE_CONTACT_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Get a contact via Contact ID",
    description = "Returns a contact's basic details",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Contact's basic details returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to retrieve a contact's details",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to retrieve a contact's details",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Contact not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getContact(
    @Schema(description = "The ID of the contact", example = "57392371")
    @PathVariable
    contactId: Long,
  ): ContactDto {
    log.debug("getContact called with contactId: {}", contactId)

    return contactsService.getContactByContactId(contactId)
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(CONTACT_SEARCH_PATH)
  @Operation(
    summary = "Search for contacts with a potential relationship to a prisoner",
    description = "Returns contact details (including relationship details of prisoner if prisonerId provided).",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Contact information returned, including relationship details of prisoner if prisonerId provided",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to search for contacts with a potential relationship to a prisoner",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to search for contacts with a potential relationship to a prisoner",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun searchContacts(
    @RequestParam(required = true)
    @Parameter(`in` = ParameterIn.QUERY, description = "Contact IDs. Comma-separated list of contact IDs, e.g. contactIds=123,456,789", example = "123,456,789", required = true)
    contactIds: List<Long>,
    @RequestParam(required = false)
    @Parameter(`in` = ParameterIn.QUERY, description = "The unique identifier of the prisoner (e.g. prisonNumber, prisonerNumber, prisonId). If provided, relationship information will be included between contacts and prisoner", example = "AA123456", required = false)
    prisonerId: String? = null,
    @RequestParam(required = false)
    @Parameter(description = "Defaults to false. Returns all contacts restrictions if set to true, skips grabbing restrictions if false", example = "false")
    withRestrictions: Boolean? = false,
  ): List<ContactWithOptionalPrisonerRelationshipDto> {
    log.debug("searchContacts called with params : Prisoner: {}, contactIds = {}, withRestrictions = {}", prisonerId, contactIds, withRestrictions)

    return contactsService.searchContacts(
      contactIds = contactIds,
      prisonerId = prisonerId,
      withRestrictions = withRestrictions ?: false,
    )
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(GET_CONTACT_LINKED_PRISONERS_CONTROLLER_PATH)
  @Operation(
    summary = "Get a contact's linked prisoners via Contact ID",
    description = "Returns a contact's linked prisoners (prisonerIds) as a list",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Contact's linked prisoners list returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to retrieve a contact's linked prisoners",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to retrieve a contact's linked prisoners",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Contact not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getContactLinkedPrisoners(
    @Schema(description = "The ID of the contact", example = "57392371")
    @PathVariable
    contactId: Long,
  ): List<ContactLinkedPrisonerDto> {
    log.debug("getContactLinkedPrisoners called with contactId: {}", contactId)

    return contactsService.getContactLinkedPrisonersByContactId(contactId)
  }

  @PreAuthorize("hasRole('PRISONER_CONTACT_REGISTRY')")
  @GetMapping(GET_CONTACT_GLOBAL_RESTRICTIONS_CONTROLLER_PATH)
  @Operation(
    summary = "Get a contact's global restrictions",
    description = "Returns a contact's global restrictions",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Contact's global restrictions returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to retrieve a contact's global restrictions",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to retrieve a contact's global restrictions",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Contact not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getContactGlobalRestrictions(
    @Schema(description = "The ID of the contact whose global restrictions are sought.", example = "57392371")
    @PathVariable
    contactId: Long,
  ): List<RestrictionDto> {
    log.debug("getContactGlobalRestrictions called with params : contactId: {}", contactId)

    return contactsService.getContactGlobalRestrictions(contactId)
  }
}
