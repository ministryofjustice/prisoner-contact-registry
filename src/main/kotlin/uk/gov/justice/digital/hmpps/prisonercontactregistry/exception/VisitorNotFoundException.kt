package uk.gov.justice.digital.hmpps.prisonercontactregistry.exception

import java.util.function.Supplier

class VisitorNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<VisitorNotFoundException> {
  override fun get(): VisitorNotFoundException = VisitorNotFoundException(message, cause)
}
