package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsPrisonerContactDto

@Service
class PrisonerContactService(private val personalRelationshipsApiClient: PersonalRelationshipsApiClient) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonerContacts(
    prisonerId: String,
    approvedContactsOnly: Boolean,
  ): List<PersonalRelationshipsPrisonerContactDto> = personalRelationshipsApiClient.getPrisonerContacts(
    prisonerId = prisonerId,
    approvedVisitorOnly = approvedContactsOnly,
  )

  fun getPrisonerContactViaRelationship(
    prisonerId: String,
    contactId: String,
    relationshipId: Long,
  ): PersonalRelationshipsPrisonerContactDto? = personalRelationshipsApiClient.getPrisonerContactViaRelationshipId(
    prisonerId = prisonerId,
    contactId = contactId,
    relationshipId = relationshipId,
  )
}
