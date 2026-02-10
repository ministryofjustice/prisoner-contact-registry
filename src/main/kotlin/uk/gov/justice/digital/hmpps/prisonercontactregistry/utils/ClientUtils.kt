package uk.gov.justice.digital.hmpps.prisonercontactregistry.utils

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException

@Component
class ClientUtils {
  fun isNotFoundError(e: Throwable?) = e is WebClientResponseException && e.statusCode.value() == 404
}
