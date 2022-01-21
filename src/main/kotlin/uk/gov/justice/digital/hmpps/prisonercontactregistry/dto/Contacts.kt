package uk.gov.justice.digital.hmpps.prisonercontactregistry.dto

data class Contacts(
  var offenderContacts: List<Contact> = listOf()
)
