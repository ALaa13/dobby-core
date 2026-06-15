package com.example.dobby.service

import com.example.dobby.AppProperties
import com.example.dobby.exception.DobbyException
import org.springframework.stereotype.Service

@Service
class DevService(
    private val appProperties: AppProperties,
    private val jwtService: JWTService
) {
    fun getTestToken(secreteCode: String): String {
        val devSecret = appProperties.dev.secretKey
        if (secreteCode != devSecret) {
            throw DobbyException.AuthorizationException("Invalid secret")
        }
        return jwtService.generateJWTToken("dev", "dev@example.com")
    }
}