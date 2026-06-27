package com.example.dobby.exception

import com.example.dobby.dto.ApiResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    // @Valid DTO/Body validation failures
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse> {
        val details =
            e.bindingResult.fieldErrors.joinToString(", ") {
                "${it.field}: ${it.defaultMessage}"
            }

        return ResponseEntity.badRequest().body(
            ApiResponse(success = false, message = "Validation failed: $details"),
        )
    }

    // Catches RequestParam / PathVariable validation failures (e.g., @Size, @NotBlank)
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintExceptions(e: ConstraintViolationException): ResponseEntity<ApiResponse> {
        val details =
            e.constraintViolations.joinToString(", ") { violation ->
                val paramName = violation.propertyPath.toString().substringAfterLast(".")
                "$paramName: ${violation.message}"
            }

        return ResponseEntity.badRequest().body(
            ApiResponse(success = false, message = "Invalid parameters: $details"),
        )
    }

    // Catches when a required @RequestParam is completely omitted from the URL
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParams(e: MissingServletRequestParameterException): ResponseEntity<ApiResponse> =
        ResponseEntity.badRequest().body(
            ApiResponse(
                success = false,
                message = "Required parameter '${e.parameterName}' is completely missing",
            ),
        )

    // Dobby-specific exceptions
    @ExceptionHandler(DobbyException::class)
    fun handleDobbyExceptions(e: DobbyException): ResponseEntity<ApiResponse> {
        val (status, message) =
            when (e) {
                is DobbyException.DatabaseException ->
                    HttpStatus.INTERNAL_SERVER_ERROR to "Database operation failed. Reason: ${e.message}"

                is DobbyException.NetworkTimeoutException ->
                    HttpStatus.GATEWAY_TIMEOUT to "The database took too long to wake up."

                is DobbyException.AiModelException ->
                    HttpStatus.BAD_GATEWAY to "Failed to get a response from the AI brain."

                is DobbyException.ProfileNotFoundException ->
                    HttpStatus.NOT_FOUND to "User profile not found."

                is DobbyException.AuthorizationException ->
                    HttpStatus.UNAUTHORIZED to "You are not authorized to perform this action."

                is DobbyException.InvalidAuthenticationRequestException ->
                    HttpStatus.BAD_REQUEST to "Invalid authentication request."

                is DobbyException.JWTException ->
                    HttpStatus.UNAUTHORIZED to "Invalid or expired JWT token."

                is DobbyException.LogStreamException ->
                    HttpStatus.INTERNAL_SERVER_ERROR to "Failed to stream logs: ${e.message}"

                is DobbyException.DataMappingException,
                is DobbyException.GeneralException,
                ->
                    HttpStatus.INTERNAL_SERVER_ERROR to "An unexpected internal system error occurred."
            }

        return ResponseEntity
            .status(status)
            .body(ApiResponse(success = false, message = message))
    }
}
