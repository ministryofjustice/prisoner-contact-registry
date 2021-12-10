package uk.gov.justice.digital.hmpps.prisonercontactregistry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.ContactDTO
import uk.gov.justice.digital.hmpps.prisonercontactregistry.dto.RestrictionDto
import java.time.LocalDate
import java.util.function.Supplier

@Service
class PrisonerContactRegistryService {

  fun getPingMsg(): String {
    return "PONG from service!"
  }

  fun getContactById(nomisPersonId: String): ContactDTO {
    // TODO: ContactDTO -> aggregate dto or update to PrisonAPI, just stub data for now
    return ContactDTO.Builder(nomisPersonId)
      .firstName("Phil")
      .lastName("Milne")
      .dateOfBirth(LocalDate.now())
      .contactType("Simple Type")
      .contactTypeDescription("Mock Contact Data")
      .restrictions(
        listOf(
          RestrictionDto.Builder()
            .restrictionType("Basic Type")
            .restrictionTypeDescription("Mock Restriction Data")
            .startDate(LocalDate.now())
            .build()
        )
      )
      .build()
  }
}

class PersonNotFoundException(message: String?) :
  RuntimeException(message),
  Supplier<PersonNotFoundException> {
  override fun get(): PersonNotFoundException {
    return PersonNotFoundException(message)
  }
}
