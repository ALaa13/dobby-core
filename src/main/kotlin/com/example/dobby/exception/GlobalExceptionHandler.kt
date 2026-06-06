package com.example.dobby.exception

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
    fun handleDobbyExceptions(e: DobbyException): ResponseEntity<String> {
        return when (e) {
            is DobbyException.DatabaseException -> {
                ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR) // 500
                    .body("Database operation failed. Reason: ${e.message}")
            }

            is DobbyException.NetworkTimeoutException -> {
                ResponseEntity
                    .status(HttpStatus.GATEWAY_TIMEOUT) // 504
                    .body("The database took too long to wake up.")
            }

            is DobbyException.AiModelException -> {
                ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY) // 502
                    .body("Failed to get a response from the AI brain.")
            }

            is DobbyException.ProfileNotFoundException -> {
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND) // 404
                    .body("User profile not found.")
            }

            is DobbyException.AuthorizationException -> {
                ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED) // 401
                    .body("You are not authorized to perform this action.")
            }

            is DobbyException.JWTException -> {
                ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED) // 401
                    .body("Invalid or expired JWT token.")
            }

            is DobbyException.DataMappingException,
            is DobbyException.GeneralException -> {
                ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR) // 500
                    .body("An unexpected internal system error occurred.")
            }
        }
    }
}