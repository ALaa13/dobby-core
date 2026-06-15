package com.example.dobby.exception

import com.example.dobby.dto.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(e: MethodArgumentNotValidException): ResponseEntity<Map<String, String?>> {
        val errors = e.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        return ResponseEntity.badRequest().body(errors)
    }


    @ExceptionHandler(DobbyException::class)
    fun handleDobbyExceptions(e: DobbyException): ResponseEntity<ApiResponse> {
        val (status, message) = when (e) {
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
            is DobbyException.GeneralException ->
                HttpStatus.INTERNAL_SERVER_ERROR to "An unexpected internal system error occurred."
        }

        return ResponseEntity
            .status(status)
            .body(ApiResponse(success = false, message = message))
    }
}