package uk.gov.justice.digital.hmpps.prisonercontactregistry.config

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.DateRangeNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.PersonNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.prisonercontactregistry.exception.VisitorNotFoundException

@RestControllerAdvice
class PrisonerContactRegistryExceptionHandler {
  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.info("Forbidden (403) returned with message {}", e.message)
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(
        ErrorResponse(
          status = HttpStatus.FORBIDDEN,
          userMessage = "Access denied",
        ),
      )
  }

  @ExceptionHandler(WebClientResponseException::class)
  fun handleWebClientResponseException(e: WebClientResponseException): ResponseEntity<ByteArray> {
    if (e.statusCode.is4xxClientError) {
      log.info("Unexpected client exception with message {}", e.message)
    } else {
      log.error("Unexpected server exception", e)
    }
    return ResponseEntity
      .status(e.statusCode)
      .body(e.responseBodyAsByteArray)
  }

  @ExceptionHandler(WebClientException::class)
  fun handleWebClientException(e: WebClientException): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = HttpStatus.INTERNAL_SERVER_ERROR,
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(
        ErrorResponse(
          status = HttpStatus.BAD_REQUEST,
          userMessage = "Validation failure: ${e.cause?.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleValidationException(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
    log.info("Bad Request missing parameter {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(
        ErrorResponse(
          status = (HttpStatus.BAD_REQUEST),
          userMessage = "Missing Request Parameter: ${e.cause?.message}",
          developerMessage = (e.message),
        ),
      )
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleValidationException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse>? {
    log.info("Bad Request invalid argument {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(
        ErrorResponse(
          status = HttpStatus.BAD_REQUEST,
          userMessage = "Invalid Argument: ${e.cause?.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(PrisonerNotFoundException::class)
  fun handlePrisonerNotFoundException(e: PrisonerNotFoundException): ResponseEntity<ErrorResponse>? {
    log.debug("Prisoner not found exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "Prisoner not found: ${e.cause?.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(VisitorNotFoundException::class)
  fun handleVisitorNotFoundException(e: VisitorNotFoundException): ResponseEntity<ErrorResponse>? {
    log.debug("One of the visitors provided could not be found exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "One of the visitors provided could not found",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(DateRangeNotFoundException::class)
  fun handleDateRangeNotFoundException(e: DateRangeNotFoundException): ResponseEntity<ErrorResponse>? {
    log.debug(
      "Visitor provided has a BAN restriction with either no expiry or expiry after toDate, no suitable date range found: {}",
      e.message,
    )
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "One of the visitors provided has a BAN restriction, no suitable date range found",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(PersonNotFoundException::class)
  fun handlePersonNotFoundException(e: PersonNotFoundException): ResponseEntity<ErrorResponse>? {
    log.debug("Person not found exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "Person not found: ${e.cause?.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = HttpStatus.INTERNAL_SERVER_ERROR,
          developerMessage = e.message,
        ),
      )
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage)
}
