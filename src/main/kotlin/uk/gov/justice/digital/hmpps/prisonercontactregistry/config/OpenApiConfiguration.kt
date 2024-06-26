package uk.gov.justice.digital.hmpps.prisonercontactregistry.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {

  private val buildName: String = buildProperties.name
  private val buildVersion: String = buildProperties.version

  @Value("\${info.app.description}")
  private val description: String = "Service for managing and sorting details about prison visitors. Currently a facade over visitor-orientated endpoints of the Prison API"

  @Value("\${info.app.contact.name}")
  private val contactName: String = "Prison Visits Booking Project"

  @Value("\${info.app.contact.email}")
  private val contactEmail: String = "prisonvisitsbooking@digital.justice.gov.uk"

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://prisoner-contact-registry.prison.service.justice.gov.uk").description("Prod"),
        Server().url("https://prisoner-contact-registry-preprod.prison.service.justice.gov.uk").description("PreProd"),
        Server().url("https://prisoner-contact-registry-staging.prison.service.justice.gov.uk").description("Staging"),
        Server().url("https://prisoner-contact-registry-dev.prison.service.justice.gov.uk").description("Development"),
        Server().url("http://localhost:8082").description("Local"),
      ),
    )
    .info(
      Info().title(buildName)
        .version(buildVersion)
        .description(description)
        .contact(Contact().name(contactName).email(contactEmail)),
    )
}
