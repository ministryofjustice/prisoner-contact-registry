package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.ContactLinkedPrisonerDto

@Service
class ContactsService(private val personalRelationshipsApiClient: PersonalRelationshipsApiClient) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getContactByContactId(contactId: Long): ContactDto {
    log.debug("getContactByContactId called with parameters : contactId - {}", contactId)
    return personalRelationshipsApiClient.getContact(contactId)
  }

  fun getContactLinkedPrisonersByContactId(contactId: Long): List<ContactLinkedPrisonerDto> {
    log.debug("getContactLinkedPrisonersByContactId called with parameters : contactId - {}", contactId)
    return personalRelationshipsApiClient.getContactLinkedPrisoners(contactId).filter { it -> it.relationshipTypeCode == "S" }
  }

  fun getContactGlobalRestrictions(
    contactId: Long,
  ): List<RestrictionDto> {
    log.debug("getContactGlobalRestrictions called with parameters : contactId {}", contactId)
    return personalRelationshipsApiClient.getContactGlobalRestrictions(contactId).map { RestrictionDto(it) }
  }
}
