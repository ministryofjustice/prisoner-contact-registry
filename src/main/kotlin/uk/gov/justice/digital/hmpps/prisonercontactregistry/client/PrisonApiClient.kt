package uk.gov.justice.digital.hmpps.prisonercontactregistry.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.AddressDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactsDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.service.PrisonerNotFoundException
import java.time.Duration

@Component
class PrisonApiClient(
  @Qualifier("prisonApiWebClient")
  private val webClient: WebClient,
  @Value("\${prison.api.timeout:60s}")
  val apiTimeout: Duration,
) {

  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val contacts = object : ParameterizedTypeReference<ContactsDto>() {}
  private val addresses = object : ParameterizedTypeReference<List<AddressDto>>() {}

  fun getOffenderContacts(offenderNo: String, approvedVisitorsOnly: Boolean): ContactsDto {
    var uri = "/api/offenders/$offenderNo/contacts"

    if (approvedVisitorsOnly) {
      uri += "?approvedVisitorsOnly=true"
    }

    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono(contacts)
      .onErrorResume {
          e ->
        if (!isNotFoundError(e)) {
          logger.error("get offender contacts Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("get offender contacts returned NOT_FOUND for get request $uri")
          Mono.error { PrisonerNotFoundException("Contacts not found for - $offenderNo on prison-api") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { PrisonerNotFoundException("Contacts not found for - $offenderNo on prison-api") }
      .also {
        logger.debug("Get offender contacts called for {}, approvedVisitorsOnly - {}", offenderNo, approvedVisitorsOnly)
      }
  }

  fun getPersonAddress(personId: Long): List<AddressDto>? {
    return webClient.get()
      .uri("/api/persons/$personId/addresses")
      .retrieve()
      .bodyToMono(addresses)
      .block(apiTimeout)
      .also {
        logger.debug("Get person address called for $personId")
      }
  }

  fun isNotFoundError(e: Throwable?) =
    e is WebClientResponseException && e.statusCode == HttpStatus.NOT_FOUND
}
