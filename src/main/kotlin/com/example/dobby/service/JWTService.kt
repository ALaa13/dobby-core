package com.example.dobby.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.dobby.AppProperties
import com.example.dobby.config.log
import com.example.dobby.exception.DobbyException
import org.springframework.stereotype.Service
import java.util.Date

@Service
class JWTService(
    private val appProperties: AppProperties,
) {
    fun generateJWTToken(
        subject: String,
        claim: String,
    ): String {
        val jwtSecret = appProperties.jwt.secret
        val jwtExpiration = appProperties.jwt.expiration

        return try {
            log.info("Generating JWT token for subject: $subject, claim: $claim")
            val algorithm = Algorithm.HMAC256(jwtSecret)
            JWT
                .create()
                .withIssuer("dobby-core")
                .withSubject(subject)
                .withClaim("username", claim)
                .withExpiresAt(Date(System.currentTimeMillis() + jwtExpiration.toMillis()))
                .sign(algorithm)
        } catch (e: Exception) {
            log.error("Failed to generate JWT token", e)
            throw DobbyException.JWTException("Failed to generate JWT token: ${e.message}", e)
        }
    }

    fun validateTokenAndGetSubject(token: String): String {
        val jwtSecret = appProperties.jwt.secret

        return try {
            log.info("Validating JWT token")
            val algorithm = Algorithm.HMAC256(jwtSecret)
            val verifier =
                JWT
                    .require(algorithm)
                    .withIssuer("dobby-core")
                    .build()
            val decodedJWT = verifier.verify(token)
            decodedJWT.subject
        } catch (e: Exception) {
            log.error("Failed to validate JWT token", e)
            throw DobbyException.JWTException("Failed to validate JWT token: ${e.message}", e)
        }
    }
}
