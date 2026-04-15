package uk.gov.justice.digital.hmpps.prisonercontactregistry.controller

import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonercontactregistry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.service.ContactsService

const val CONTACTS_CONTROLLER_PATH: String = "v2/contacts/{contactId}"
const val GET_CONTACT_RESTRICTIONS_CONTROLLER_PATH: String = "$CONTACTS_CONTROLLER_PATH/restrictions"

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
  @GetMapping(GET_CONTACT_RESTRICTIONS_CONTROLLER_PATH)
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
