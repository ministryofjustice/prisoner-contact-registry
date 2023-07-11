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

@RestController
@Validated
@RequestMapping(name = "Contact Resource", path = ["/prisoners"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerContactController(
  private val contactService: PrisonerContactRegistryService,
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

  private fun orderByLastNameAndThenFirstName(contactList: List<ContactDto>): List<ContactDto> {
    return contactList.sortedWith(compareBy({ it.lastName }, { it.firstName }))
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
