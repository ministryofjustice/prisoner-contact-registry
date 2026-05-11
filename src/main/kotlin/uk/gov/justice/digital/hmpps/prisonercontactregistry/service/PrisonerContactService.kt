package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.PersonalRelationshipsPrisonerContactDto

@Service
class PrisonerContactService(private val personalRelationshipsApiClient: PersonalRelationshipsApiClient) {
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

  fun searchContacts(
    prisonerId: String,
    contactIds: List<Long>,
  ) = personalRelationshipsApiClient.searchContact(prisonerId, contactIds)
}
