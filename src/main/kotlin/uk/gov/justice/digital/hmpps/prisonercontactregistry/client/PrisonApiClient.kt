package uk.gov.justice.digital.hmpps.prisonercontactregistry.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class PrisonApiClient(@Qualifier("prisonApiWebClient") private val webClient: WebClient)
