package uk.gov.justice.digital.hmpps.prisonercontactregistry.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.AddressDto
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactsDto
import java.time.Duration

@Component
class PrisonApiClient(
  @Qualifier("prisonApiWebClient")
  private val webClient: WebClient,
  @Value("\${prison.api.timeout:60s}")
  val apiTimeout: Duration,
) {

  private val contacts = object : ParameterizedTypeReference<ContactsDto>() {}
  private val addresses = object : ParameterizedTypeReference<List<AddressDto>>() {}

  fun getOffenderContacts(offenderNo: String, approvedVisitorsOnly: Boolean? = null): ContactsDto? {
    var uri = "/api/offenders/$offenderNo/contacts"

    if (approvedVisitorsOnly != null && approvedVisitorsOnly) {
      uri += "?approvedVisitorsOnly=true"
    }

    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono(contacts)
      .block(apiTimeout)
      .also {
        log.debug("Get offender contacts called for {}, approvedVisitorsOnly - {}", offenderNo, approvedVisitorsOnly)
      }
  }

  fun getPersonAddress(personId: Long): List<AddressDto>? {
    return webClient.get()
      .uri("/api/persons/$personId/addresses")
      .retrieve()
      .bodyToMono(addresses)
      .block(apiTimeout)
      .also {
        log.debug("Get person address called for $personId")
      }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
