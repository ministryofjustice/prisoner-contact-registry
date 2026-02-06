package uk.gov.justice.digital.hmpps.prisonercontactregistry.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import uk.gov.justice.hmpps.kotlin.auth.service.GlobalPrincipalOAuth2AuthorizedClientService
import java.time.Duration

private const val CLIENT_REGISTRATION_ID = "prison-api"

@Configuration
class WebClientConfiguration(
  @param:Value("\${prison.api.url}")
  private val prisonApiBaseUrl: String,

  @param:Value("\${personal-relationships.api.url}")
  private val personalRelationshipsApiBaseUrl: String,

  @param:Value("\${api.timeout:10s}") private val apiTimeout: Duration,
  @param:Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
) {
  @Bean
  fun prisonApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(prisonApiBaseUrl, authorizedClientManager, builder)

  @Bean
  fun prisonApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonApiBaseUrl, healthTimeout)

  @Bean
  fun personalRelationshipsApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(personalRelationshipsApiBaseUrl, authorizedClientManager, builder)

  @Bean
  fun personalRelationshipsApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(personalRelationshipsApiBaseUrl, healthTimeout)

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
  ): OAuth2AuthorizedClientManager {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      GlobalPrincipalOAuth2AuthorizedClientService(clientRegistrationRepository),
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  private fun getWebClient(
    url: String,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    url = url,
    registrationId = CLIENT_REGISTRATION_ID,
    timeout = apiTimeout,
  )
}
