package uk.gov.justice.digital.hmpps.prisonercontactregistry.controller

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.prisonercontactregistry.integration.IntegrationTestBase

internal class PrisonerContactRegistryResourceTest : IntegrationTestBase() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @BeforeEach
  fun setUp() {
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun `get Ping It`() {
    // needs a mock
  }

  @Test
  fun `get Ping Auth`() {
  }
}
