package uk.gov.justice.digital.hmpps.prisonercontactregistry.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${prison.api.url}")
  private val prisonApiBaseUrl: String,

  @param:Value("\${prison.api.timeout:10s}") private val apiTimeout: Duration,
  @param:Value("\${prison.api.health-timeout:2s}") val healthTimeout: Duration,
) {
  private val clientRegistrationId = "prison-api"

  @Bean
  fun prisonApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = clientRegistrationId, url = prisonApiBaseUrl, apiTimeout)

  @Bean
  fun prisonApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonApiBaseUrl, healthTimeout)
}
