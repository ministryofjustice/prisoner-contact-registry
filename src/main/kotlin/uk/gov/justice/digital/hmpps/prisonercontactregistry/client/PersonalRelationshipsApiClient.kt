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
  @param:Value("\${api.timeout:60s}")
  private val apiTimeout: Duration,
  private val clientUtils: ClientUtils,
) {

  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonerContacts(prisonerId: String): List<ContactDto> {
    val uri = "/prisoner/$prisonerId/contact"

    logger.info("Get prisoner contacts called for $prisonerId, via the personal-relationships-api")

    // 1 - Get the contacts
    val restPageResponse = webClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path(uri)
          .queryParam("relationshipType", "S")
          .queryParam("page", 0)
          .queryParam("size", 100) // TODO VB-5969: What would be the best size here? Also should "active" be set?
          .build(prisonerId)
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
      .blockOptional(apiTimeout).orElseThrow { IllegalStateException("Timeout getting contact for - $prisonerId on personal-relationships-api") }

    logger.info("Get prisoner contacts called for $prisonerId, via the personal-relationships-api returned ${restPageResponse.content.size} contacts, relationshipType = S")

    // 2 - Get the "prisoner-contact" restrictions
    val allPrisonerContactRestrictions = getPrisonerContactRestrictions(restPageResponse.content.map { it.prisonerContactId })

    // 3 - Convert the contacts + restrictions into the expected ContactDto shape
    return convertToContactDto(restPageResponse.content, allPrisonerContactRestrictions)
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

  private fun convertToContactDto(prisonerContactsList: List<PersonalRelationshipsContactDto>, prisonerContactRestrictions: PrisonerContactRestrictionsResponseDto): List<ContactDto> {
    // Pre-index once: contactId -> restrictions to avoid multiple loops over these restrictions per contact in the next step.
    val restrictionsByContactId: Map<Long, List<RestrictionDto>> =
      prisonerContactRestrictions.prisonerContactRestrictions
        .flatMap { group ->
          val prisonerScoped = group.prisonerContactRestrictions.map { r ->
            r.contactId to RestrictionDto(
              restrictionId = r.prisonerContactRestrictionId.toInt(),
              restrictionType = r.restrictionType,
              restrictionTypeDescription = r.restrictionTypeDescription,
              startDate = r.startDate,
              expiryDate = r.expiryDate,
              globalRestriction = false,
              comment = r.comments,
            )
          }

          val global = group.globalContactRestrictions.map { r ->
            r.contactId to RestrictionDto(
              restrictionId = r.contactRestrictionId.toInt(),
              restrictionType = r.restrictionType,
              restrictionTypeDescription = r.restrictionTypeDescription,
              startDate = r.startDate,
              expiryDate = r.expiryDate,
              globalRestriction = true,
              comment = r.comments,
            )
          }

          prisonerScoped + global
        }
        // This groupBy forms the Map<contactId, List<RestrictionDto>>
        .groupBy(
          keySelector = { it.first },
          valueTransform = { it.second },
        )

    logger.info("Converted contact restrictions into Map (contactId -> List<Restrictions>), restrictionsByContactId size = ${restrictionsByContactId.size}")

    return prisonerContactsList.map { c ->
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
        addresses = listOf(), // Set separately via a different call to get contacts addresses (Data not present in getPrisonerContacts call)
        restrictions = restrictionsByContactId[c.contactId].orEmpty(), // Map contacts using lookup to the previously grouped restrictions map in step 1
      )
    }
  }
}
