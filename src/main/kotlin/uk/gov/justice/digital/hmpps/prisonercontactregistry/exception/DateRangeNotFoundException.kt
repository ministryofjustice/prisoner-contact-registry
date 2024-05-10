package uk.gov.justice.digital.hmpps.prisonercontactregistry.exception

import java.util.function.Supplier

class DateRangeNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<DateRangeNotFoundException> {
  override fun get(): DateRangeNotFoundException {
    return DateRangeNotFoundException(message, cause)
  }
}
