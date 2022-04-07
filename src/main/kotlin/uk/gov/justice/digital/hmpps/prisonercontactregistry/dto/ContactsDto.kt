package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

data class ContactsDto(
  var offenderContacts: List<ContactDto> = listOf()
)
