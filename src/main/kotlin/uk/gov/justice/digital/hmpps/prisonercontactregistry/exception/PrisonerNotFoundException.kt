package uk.gov.justice.digital.hmpps.prisonercontactregistry.exception

import java.util.function.Supplier

class PrisonerNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<PrisonerNotFoundException> {
  override fun get(): PrisonerNotFoundException {
    return PrisonerNotFoundException(message, cause)
  }
}
