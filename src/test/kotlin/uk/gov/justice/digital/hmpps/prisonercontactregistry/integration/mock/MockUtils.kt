package uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.mock

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import org.springframework.http.MediaType

class MockUtils {
  companion object {
    fun createJsonResponseBuilder(): ResponseDefinitionBuilder {
      return WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
    }
  }
}
