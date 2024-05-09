package uk.gov.justice.digital.hmpps.prisonercontactregistry.exception

import java.util.function.Supplier

class PersonNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<PersonNotFoundException> {
  override fun get(): PersonNotFoundException {
    return PersonNotFoundException(message, cause)
  }
}
