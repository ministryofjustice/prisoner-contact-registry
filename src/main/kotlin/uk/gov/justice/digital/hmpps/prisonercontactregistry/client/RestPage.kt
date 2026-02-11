package uk.gov.justice.digital.hmpps.prisonercontactregistry.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class PageMetadata(
  @param:JsonProperty("size") val size: Int = 0,
  @param:JsonProperty("number") val number: Int = 0,
  @param:JsonProperty("totalElements") val totalElements: Long = 0,
  @param:JsonProperty("totalPages") val totalPages: Int = 0,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PagedResponse<T>(
  @param:JsonProperty("content") val content: List<T> = emptyList(),
  @param:JsonProperty("page") val page: PageMetadata = PageMetadata(),
)
