package com.example.dobby.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.dobby.exception.DobbyException
import com.example.dobby.util.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class JWTService(
    @Value($$"${jwt.secret}") private val jwtSecret: String,
    @Value($$"${jwt.expiration}") private val jwtExpiration: Duration,
) {

    fun generateJWTToken(subject: String, claim: String): String {
        return try {
            val algorithm = Algorithm.HMAC256(jwtSecret)
            JWT.create()
                .withIssuer("dobby-core")
                .withSubject(subject)
                .withClaim("username", claim)
                .withExpiresAt(Date(System.currentTimeMillis() + jwtExpiration.toMillis()))
                .sign(algorithm)
        } catch (e: Exception) {
            logger.error("Failed to generate JWT token", e)
            throw DobbyException.JWTException("Failed to generate JWT token: ${e.message}", e)
        }
    }

    fun validateTokenAndGetSubject(token: String): String {
        return try {
            val algorithm = Algorithm.HMAC256(jwtSecret)
            val verifier = JWT.require(algorithm)
                .withIssuer("dobby-core")
                .build()
            val decodedJWT = verifier.verify(token)
            decodedJWT.subject
        } catch (e: Exception) {
            logger.error("Failed to validate JWT token", e)
            throw DobbyException.JWTException("Failed to validate JWT token: ${e.message}", e)
        }
    }
}