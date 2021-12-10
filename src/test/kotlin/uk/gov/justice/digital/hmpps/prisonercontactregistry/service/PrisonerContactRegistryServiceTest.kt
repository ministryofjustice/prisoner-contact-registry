package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class PrisonerContactRegistryServiceTest {

  // TODO: research late init..
  private lateinit var contactService: PrisonerContactRegistryService

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @BeforeEach
  fun setUp() {
    contactService = PrisonerContactRegistryService()
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun getPingMsg() {
    val msg = contactService.getPingMsg()
    assertThat(msg).isEqualTo("PONG from service!")
  }
}
