package com.example.dobby.service

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bucket
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.TimeUnit


private const val RATE_LIMIT_TOKENS = 10L

@Service
class RateLimitingService {
    private val cache = Caffeine.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build<String, Bucket>()

    fun tryConsume(callerId: String): Boolean {
        val bucket = cache.get(callerId) { createNewBucket() }
        return bucket.tryConsume(1)
    }

    private fun createNewBucket(): Bucket {
        return Bucket.builder()
            .addLimit { limit ->
                limit.capacity(RATE_LIMIT_TOKENS)
                    .refillGreedy(RATE_LIMIT_TOKENS, Duration.ofMinutes(1))
            }
            .build()
    }
}