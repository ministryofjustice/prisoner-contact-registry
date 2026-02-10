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
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactIdsRequestDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PrisonerContactRestrictionsResponseDto
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

  fun getPrisonerContacts(prisonerId: String, approvedVisitorOnly: Boolean): List<ContactDto> {
    logger.info("Get prisoner contacts called for $prisonerId, via the personal-relationships-api")

    // 1 - Get the contacts
    val prisonerContacts = getAllContacts(prisonerId, approvedVisitorOnly)

    logger.info("Get prisoner contacts called for $prisonerId, via the personal-relationships-api returned ${prisonerContacts.size} contacts, relationshipType = S")

    // 2 - Get the "local + global" prisoner-contact restrictions
    val allPrisonerContactRestrictions = getPrisonerContactRestrictions(prisonerContacts.map { it.prisonerContactId })

    // 3 - Convert the contacts + restrictions into the expected ContactDto shape, to preserve the existing DTO
    return convertToContactDto(prisonerContacts, allPrisonerContactRestrictions)
  }

  private fun getAllContacts(prisonerId: String, approvedVisitorOnly: Boolean): List<PersonalRelationshipsContactDto> {
    val uri = "/prisoner/$prisonerId/contact"

    return webClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path(uri)
          .queryParam("relationshipType", "S")
          .queryParam("page", 0)
          .queryParam("size", 350)
          .apply {
            // Only set this param if it's true. Setting it to false returns only unapproved visitors (we do not want this).
            if (approvedVisitorOnly) {
              queryParam("approvedVisitor", true)
            }
          }.build()
      }
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<RestPage<PersonalRelationshipsContactDto>>() {})
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
      .orElseThrow { IllegalStateException("Timeout getting contact for - $prisonerId on personal-relationships-api") }.content
  }

  private fun getPrisonerContactRestrictions(prisonerContactRelationshipIds: List<Long>): PrisonerContactRestrictionsResponseDto {
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

  /**
   * Builds ContactDto entries from Personal Relationships API data to preserve the contract we have with calling APIs.
   *
   * Notes:
   * - The same contactId can appear multiple times, each containing a different
   *   relationship to the prisoner (identified by prisonerContactId).
   * - Restrictions can be relationship-level (local) or contact-level (global).
   *
   * This method:
   * - Attaches local restrictions to the correct relationship using prisonerContactId.
   * - Attaches global restrictions to all relationships for the same contactId.
   * - Preserves duplicate contact entries where relationships differ.
   *
   * Returns:
   * - A ContactDto list [to keep the exact structure as the previous client prison-api, had]
   */

  private fun convertToContactDto(prisonerContactsList: List<PersonalRelationshipsContactDto>, prisonerContactRestrictions: PrisonerContactRestrictionsResponseDto): List<ContactDto> {
    // 1) Index LOCAL restrictions by prisonerContactId (relationship-level)
    val localByPrisonerContactId: Map<Long, List<RestrictionDto>> =
      prisonerContactRestrictions.prisonerContactRestrictions
        .associate { group ->
          group.prisonerContactId to group.prisonerContactRestrictions.map { r ->
            RestrictionDto(personalRelationshipsLocalRestriction = r)
          }
        }

    // 2) Index GLOBAL restrictions by contactId (contact-level)
    val globalByContactId: Map<Long, List<RestrictionDto>> =
      prisonerContactRestrictions.prisonerContactRestrictions
        .flatMap { group ->
          group.globalContactRestrictions.map { r ->
            r.contactId to RestrictionDto(personalRelationshipsGlobalRestriction = r)
          }
        }
        // This groupBy forms the Map<contactId, List<RestrictionDto>>
        .groupBy(
          keySelector = { it.first },
          valueTransform = { it.second },
        )

    logger.info("Indexed restrictions: localByPrisonerContactId=${localByPrisonerContactId.size}, globalByContactId=${globalByContactId.size}")

    // 3) Map contacts: relationship keeps its own local restrictions + contact's global restrictions
    return prisonerContactsList.map { c ->
      val local = localByPrisonerContactId[c.prisonerContactId].orEmpty()
      val global = globalByContactId[c.contactId].orEmpty()

      ContactDto(personalRelationshipsContact = c, restrictions = local + global, addresses = emptyList())
    }
  }
}
