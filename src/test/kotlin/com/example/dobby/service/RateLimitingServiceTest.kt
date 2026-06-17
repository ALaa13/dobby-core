package com.example.dobby.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RateLimitingServiceTest {

    private lateinit var rateLimitingService: RateLimitingService
    private val testApiKey = "test-service-key"

    @BeforeEach
    fun setUp() {
        rateLimitingService = RateLimitingService()
    }

    @Test
    fun `should allow initial requests up to the maximum capacity limit`() {
        for (i in 1..10) {
            assertTrue(
                rateLimitingService.tryConsume(testApiKey),
                "Request $i should be allowed"
            )
        }

        // The 11th request must hit the rate limit barrier instantly
        assertFalse(
            rateLimitingService.tryConsume(testApiKey),
            "The 11th request should be blocked"
        )
    }

    @Test
    fun `should maintain separate isolated buckets for different API keys`() {
        val dangerousSpammerKey = "spammer-key"
        val goodClientKey = "good-client-key"

        // Exhaust the spammer's entire bucket capacity
        for (i in 1..10) {
            rateLimitingService.tryConsume(dangerousSpammerKey)
        }

        assertFalse(rateLimitingService.tryConsume(dangerousSpammerKey))

        assertTrue(
            rateLimitingService.tryConsume(goodClientKey),
            "Good client should still be allowed through"
        )
    }
}