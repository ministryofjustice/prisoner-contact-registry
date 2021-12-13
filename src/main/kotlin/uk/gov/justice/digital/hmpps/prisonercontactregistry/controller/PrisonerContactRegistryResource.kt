package uk.gov.justice.digital.hmpps.prisonercontactregistry.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonercontactregistry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDTO
import uk.gov.justice.digital.hmpps.prisonercontactregistry.service.PrisonerContactRegistryService

@RestController
@RequestMapping(name = "Contact Resource", path = ["/prisoner-contacts"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerContactRegistryResource(
  private val contactService: PrisonerContactRegistryService
) {

  @GetMapping("/ping")
  @Operation(
    summary = "Get Ping",
    description = "Retrieve Ping Message",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Ping Msg Returned"
      )
    ]
  )
  fun getPing(): String = contactService.getPingMsg()

  // @PreAuthorize("hasRole('PRISONER_CONTACT')") - use visits/dev to start
  // @PreAuthorize("hasRole('VIEW_PRISONER_DATA')")
  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @GetMapping("{nomisPersonId}")
  @Operation(
    summary = "Get Prisoner Contact",
    description = "Returns details of a prisoner contact",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner Contact Information Returned"
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get Prisoner Contact for Person Identifier",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions retrieve a Prisoner Contact",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner Contact not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  fun getPrisonerContact(
    @Schema(description = "NOMIS Person Identifier", example = "ID12345", required = true)
    @PathVariable nomisPersonId: String
  ): ContactDTO = contactService.getContactById(nomisPersonId)
}
