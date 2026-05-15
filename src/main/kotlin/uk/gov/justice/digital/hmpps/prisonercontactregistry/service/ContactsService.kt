package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonercontactregistry.client.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactWithOptionalPrisonerRelationshipDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.ContactLinkedPrisonerDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.personal.relationships.toContactWithOptionalPrisonerRelationshipDto

@Service
class ContactsService(
  private val restrictionsService: RestrictionsService,
  private val personalRelationshipsApiClient: PersonalRelationshipsApiClient,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val SOCIAL_CONTACT_TYPE_CODE = "S"
  }

  fun getContactByContactId(contactId: Long): ContactDto {
    log.debug("getContactByContactId called with parameters : contactId - {}", contactId)
    return personalRelationshipsApiClient.getContact(contactId)
  }

  fun searchContacts(contactIds: List<Long>, prisonerId: String? = null, withRestrictions: Boolean): List<ContactWithOptionalPrisonerRelationshipDto> {
    log.debug(
      "searchContacts called with parameters : prisonerId - {}, contactIds - {}, withRestrictions - {}",
      prisonerId,
      contactIds,
      withRestrictions,
    )

    val foundContacts = personalRelationshipsApiClient.searchContact(prisonerId, contactIds)

    val contacts = foundContacts.flatMap { contact ->
      contact.toContactWithOptionalPrisonerRelationshipDto().filter { it.contactType != "O" }
    }

    if (!withRestrictions) {
      return contacts
    }

    val indexedGlobalAndLocalRestrictions = restrictionsService.getContactsGlobalAndLocalRestrictions(
      contacts
        .mapNotNull { it.prisonerContactId }
        .distinct(),
    )

    // Because local restrictions are bound between prisoner and contact, if relationship is null we need to do a separate
    // global restrictions lookup for these contacts.
    val globalRestrictionsByContactId = restrictionsService.getContactsGlobalRestrictions(
      contacts
        .filter { it.prisonerContactId == null }
        .map { it.contactId }
        .distinct(),
    )

    return contacts.map { contact ->
      val restrictions = if (contact.prisonerContactId != null) {
        indexedGlobalAndLocalRestrictions.forContact(
          contactId = contact.contactId,
          prisonerContactId = contact.prisonerContactId,
        )
      } else {
        globalRestrictionsByContactId[contact.contactId].orEmpty()
      }

      contact.copy(restrictions = restrictions)
    }
  }

  fun getContactLinkedPrisonersByContactId(contactId: Long): List<ContactLinkedPrisonerDto> {
    log.debug("getContactLinkedPrisonersByContactId called with parameters : contactId - {}", contactId)
    return personalRelationshipsApiClient.getContactLinkedPrisoners(contactId).filter { it -> it.relationshipTypeCode == SOCIAL_CONTACT_TYPE_CODE }
  }

  fun getContactGlobalRestrictions(contactId: Long): List<RestrictionDto> = restrictionsService.getContactGlobalRestrictions(contactId)
}
