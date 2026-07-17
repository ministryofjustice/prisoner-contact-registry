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
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.ContactIdsRequestDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.ContactLinkedPrisonerDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.ContactsRestrictionsResponseDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.GlobalContactRestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsContactSearchResultDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsPrisonerContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactIdsRequestDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.PersonNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.utils.ClientUtils
import java.time.Duration

@Component
class PersonalRelationshipsApiClient(
  @param:Qualifier("personalRelationshipsApiWebClient")
  private val webClient: WebClient,
  @param:Value("\${api.timeout:10s}")
  private val apiTimeout: Duration,
  private val clientUtils: ClientUtils,
) {

  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getContact(contactId: Long): ContactDto {
    val uri = "/contact/$contactId"

    logger.info("Get a contact using contactId called $uri, via the personal-relationships-api")

    val contact = webClient
      .get()
      .uri(uri)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<PersonalRelationshipsContactDto>() {})
      .onErrorResume { e ->
        if (!clientUtils.isNotFoundError(e)) {
          logger.error("get contact returned an error for get request $uri")
          Mono.error(e)
        } else {
          logger.error("get contact returned NOT_FOUND for get request $uri")
          Mono.error { PersonNotFoundException("Contact with id $contactId not found", cause = e) }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { IllegalStateException("Timeout getting a contact for uri $uri on personal-relationships-api") }

    return ContactDto(personalRelationshipsContact = contact)
  }

  fun getContactGlobalRestrictions(contactId: Long): List<GlobalContactRestrictionDto> {
    val uri = "/contact/$contactId/restriction"

    logger.info("Get a contact's global restrictions called $uri, via the personal-relationships-api")

    return webClient
      .get()
      .uri(uri)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<GlobalContactRestrictionDto>>() {})
      .onErrorResume { e ->
        if (!clientUtils.isNotFoundError(e)) {
          logger.error("get contact's global restrictions returned an error for get request $uri")
          Mono.error(e)
        } else {
          logger.error("get contact's global restrictions returned NOT_FOUND for get request $uri")
          Mono.error { PersonNotFoundException("Contact with id $contactId not found", cause = e) }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { IllegalStateException("Timeout getting a contact's global restrictions for uri $uri on personal-relationships-api") }
  }

  fun getContactsGlobalRestrictions(contactIds: List<Long>): ContactsRestrictionsResponseDto {
    val uri = "/contacts/restrictions"

    logger.info("Get contacts global restrictions called $uri, via the personal-relationships-api")

    return webClient
      .post()
      .uri(uri)
      .bodyValue(ContactIdsRequestDto(contactIds))
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<ContactsRestrictionsResponseDto>() {})
      .onErrorResume { e ->
        logger.error("get contacts global restrictions returned an error for post to $uri")
        Mono.error(e)
      }
      .blockOptional(apiTimeout)
      .orElseThrow {
        IllegalStateException("Timeout getting contacts global restrictions for uri $uri on personal-relationships-api")
      }
  }

  fun getContactLinkedPrisoners(contactId: Long): List<ContactLinkedPrisonerDto> {
    val uri = "/contact/$contactId/linked-prisoners"

    logger.info("Get a contact's linked prisoners using contactId called $uri, via the personal-relationships-api")

    return webClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path(uri)
          .queryParam("page", 0)
          .queryParam("size", 100)
          .build()
      }
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<PagedResponse<ContactLinkedPrisonerDto>>() {})
      .onErrorResume { e ->
        if (!clientUtils.isNotFoundError(e)) {
          logger.error("get contacts linked prisoners Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("get contacts linked prisoners returned NOT_FOUND for get request $uri")
          Mono.error { PrisonerNotFoundException("Contact not found for contactId - $contactId on personal-relationships-api") }
        }
      }
      .blockOptional(apiTimeout)
      .orElseThrow { IllegalStateException("Timeout getting contact linked prisoners for contactId $contactId on personal-relationships-api") }
      .content
  }

  fun getPrisonerContactViaRelationshipId(prisonerId: String, contactId: String, relationshipId: Long): PersonalRelationshipsPrisonerContactDto? {
    val uri = "/prisoner/$prisonerId/contact/$contactId"

    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<PersonalRelationshipsPrisonerContactDto>>() {})
      .onErrorResume { e ->
        if (!clientUtils.isNotFoundError(e)) {
          logger.error("getPrisonerContactViaRelationshipId Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("getPrisonerContactViaRelationshipId returned NOT_FOUND for get request $uri")
          Mono.error { PrisonerNotFoundException("Contact $contactId not found for prisoner $prisonerId on personal-relationships-api") }
        }
      }
      .blockOptional(apiTimeout)
      .orElseThrow { IllegalStateException("Timeout getting contact $contactId for prisoner $prisonerId on personal-relationships-api") }
      .firstOrNull { it.prisonerContactId == relationshipId }
  }

  fun getPrisonerContacts(prisonerId: String, approvedVisitorOnly: Boolean): List<PersonalRelationshipsPrisonerContactDto> {
    logger.info("Get prisoner contacts called for $prisonerId, via the personal-relationships-api")

    val uri = "/prisoner/$prisonerId/contact"

    return webClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path(uri)
          .queryParam("relationshipType", "S")
          .queryParam("page", 0)
          .queryParam("size", 450)
          .apply {
            // Only set this param if it's true. Setting it to false returns only unapproved visitors (we do not want this).
            if (approvedVisitorOnly) {
              queryParam("approvedVisitor", true)
            }
          }.build()
      }
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<PagedResponse<PersonalRelationshipsPrisonerContactDto>>() {})
      .onErrorResume { e ->
        if (!clientUtils.isNotFoundError(e)) {
          logger.error("get prisoner contacts Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("get prisoner contacts returned NOT_FOUND for get request $uri")
          Mono.error { PrisonerNotFoundException("Contacts not found for - $prisonerId on personal-relationships-api") }
        }
      }
      .blockOptional(apiTimeout)
      .orElseThrow { IllegalStateException("Timeout getting contact for - $prisonerId on personal-relationships-api") }
      .content
  }

  fun searchContact(prisonerId: String? = null, contactIds: List<Long>): List<PersonalRelationshipsContactSearchResultDto> {
    logger.info("searchContact called with contactIds ${contactIds.joinToString { it.toString() }} and prisonerId if provided = $prisonerId - via the personal-relationships-api")

    val uri = "/contact/search"

    return webClient.get()
      .uri { uriBuilder ->
        val builder = uriBuilder
          .path(uri)
          .queryParam("contactIds", contactIds.joinToString(","))
          .queryParam("searchType", "EXACT")
          .queryParam("page", 0)
          .queryParam("size", 450)

        prisonerId?.let {
          builder.queryParam("includePrisonerRelationships", it)
        }

        builder.build()
      }
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<PagedResponse<PersonalRelationshipsContactSearchResultDto>>() {})
      .onErrorResume { e ->
        logger.error("searchContact Failed for get request $uri")
        Mono.error(e)
      }
      .blockOptional(apiTimeout)
      .orElseThrow { IllegalStateException("Timeout calling searchContact - $prisonerId / ${contactIds.joinToString { it.toString() }} on personal-relationships-api") }
      .content
  }

  fun getPrisonerContactRestrictions(prisonerContactRelationshipIds: List<Long>): PrisonerContactRestrictionsResponseDto {
    val uri = "/prisoner-contact/restrictions"

    logger.info("Get prisoner contact restrictions called $uri, via the personal-relationships-api")

    return webClient
      .post()
      .uri(uri)
      .bodyValue(PrisonerContactIdsRequestDto(prisonerContactRelationshipIds))
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<PrisonerContactRestrictionsResponseDto>() {})
      .onErrorResume { e ->
        logger.error("get prisoner contacts restrictions returned an error for post to $uri")
        Mono.error(e)
      }
      .blockOptional(apiTimeout).orElseThrow { IllegalStateException("Timeout getting contact restrictions for uri $uri on personal-relationships-api") }
  }
}
