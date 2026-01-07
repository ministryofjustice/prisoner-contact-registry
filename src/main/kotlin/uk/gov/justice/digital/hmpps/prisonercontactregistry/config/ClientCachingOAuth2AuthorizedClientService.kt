package uk.gov.justice.digital.hmpps.prisonercontactregistry.config

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientId
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import java.util.concurrent.ConcurrentHashMap

class ClientCachingOAuth2AuthorizedClientService(
  private val clientRegistrationRepository: ClientRegistrationRepository,
) : OAuth2AuthorizedClientService {
  private val authorizedClients: MutableMap<OAuth2AuthorizedClientId, OAuth2AuthorizedClient> = ConcurrentHashMap()
  private val principalName = "PRISONER_CONTACT_REGISTRY"

  override fun <T : OAuth2AuthorizedClient?> loadAuthorizedClient(
    clientRegistrationId: String,
    principalName: String,
  ): T? = clientRegistrationRepository.findByRegistrationId(clientRegistrationId)?.let {
    @Suppress("UNCHECKED_CAST")
    authorizedClients[OAuth2AuthorizedClientId(clientRegistrationId, principalName)] as T
  }

  override fun saveAuthorizedClient(authorizedClient: OAuth2AuthorizedClient, principal: Authentication) {
    authorizedClients[
      OAuth2AuthorizedClientId(authorizedClient.clientRegistration.registrationId, principalName),
    ] = authorizedClient
  }

  override fun removeAuthorizedClient(clientRegistrationId: String, principalName: String) {
    clientRegistrationRepository.findByRegistrationId(clientRegistrationId)?.apply {
      authorizedClients.remove(OAuth2AuthorizedClientId(clientRegistrationId, principalName))
    }
  }
}
