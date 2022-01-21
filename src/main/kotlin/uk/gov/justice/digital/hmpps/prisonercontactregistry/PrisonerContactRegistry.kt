package uk.gov.justice.digital.hmpps.prisonercontactregistry

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@SecurityScheme(name = "bearerAuth", scheme = "bearer", type = SecuritySchemeType.HTTP, bearerFormat = "JWT")
class PrisonerContactRegistry

fun main(args: Array<String>) {
  runApplication<PrisonerContactRegistry>(*args)
}
