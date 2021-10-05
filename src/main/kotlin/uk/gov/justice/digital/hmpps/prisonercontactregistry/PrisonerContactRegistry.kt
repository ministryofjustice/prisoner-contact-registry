package uk.gov.justice.digital.hmpps.prisonercontactregistry

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class PrisonerContactRegistry

fun main(args: Array<String>) {
  runApplication<PrisonerContactRegistry>(*args)
}
