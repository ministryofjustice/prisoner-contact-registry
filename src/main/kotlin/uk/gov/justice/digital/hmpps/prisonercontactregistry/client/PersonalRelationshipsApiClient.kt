package uk.gov.justice.digital.hmpps.prisonercontactregistry.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.utils.ClientUtils
import java.time.Duration

@Component
class PersonalRelationshipsApiClient(
  @param:Qualifier("personalRelationshipsApiWebClient")
  private val webClient: WebClient,
  @param:Value("\${api.timeout:60s}")
  private val apiTimeout: Duration,
  private val clientUtils: ClientUtils,
) {

  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonerContacts(prisonerId: String): List<ContactDto> {
    var uri = "/prisoner/$prisonerId/contact"

    logger.info("Get prisoner contacts called for $prisonerId, via the personal-relationships-api")

    val prisonerContactsList = webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<PersonalRelationshipsContactDto>>() {})
      .onErrorResume { e ->
        if (!clientUtils.isNotFoundError(e)) {
          PrisonApiClient.Companion.logger.error("get prisoner contacts Failed for get request $uri")
          Mono.error(e)
        } else {
          PrisonApiClient.Companion.logger.error("get prisoner contacts returned NOT_FOUND for get request $uri")
          Mono.error { PrisonerNotFoundException("Contacts not found for - $prisonerId on personal-relationships-api") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { IllegalStateException("Timeout getting contact not found for - $prisonerId on personal-relationships-api") }

    return convertToContactDto(prisonerContactsList)
  }

  private fun convertToContactDto(prisonerContactsList: List<PersonalRelationshipsContactDto>): List<ContactDto> = prisonerContactsList.map { c ->
    ContactDto(
      personId = c.contactId,
      firstName = c.firstName,
      middleName = c.middleNames,
      lastName = c.lastName,
      dateOfBirth = c.dateOfBirth,
      relationshipCode = c.relationshipToPrisonerCode,
      relationshipDescription = c.relationshipToPrisonerDescription,
      contactType = c.relationshipTypeCode,
      contactTypeDescription = c.relationshipTypeDescription,
      approvedVisitor = c.isApprovedVisitor,
      emergencyContact = c.isEmergencyContact,
      nextOfKin = c.isNextOfKin,
      commentText = c.comments,
    )
  }
}
