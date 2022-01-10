package uk.gov.justice.digital.hmpps.prisonercontactregistry.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.Address
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.Contacts
import java.time.Duration

@Component
class PrisonApiClient(
  @Qualifier("prisonApiWebClient") private val webClient: WebClient,
  @Value("\${prison.api.timeout:60s}") val apiTimeout: Duration
) {

  private val contacts = object : ParameterizedTypeReference<Contacts>() {}
  private val addresses = object : ParameterizedTypeReference<List<Address>>() {}

  fun getOffenderContacts(offenderNo: String): Contacts? {
    return webClient.get()
      .uri("/api/offenders/$offenderNo/contacts")
      .retrieve()
      .bodyToMono(contacts)
      .block(apiTimeout)
      .also {
        log.debug("Get offender contact called")
      }
  }

  fun getPersonAddress(personId: Long): List<Address>? {
    return webClient.get()
      .uri("/api/persons/$personId/addresses")
      .retrieve()
      .bodyToMono(addresses)
      .block(apiTimeout)
      .also {
        log.debug("Get person address called")
      }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
