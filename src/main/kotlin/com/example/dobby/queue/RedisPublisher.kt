package com.example.dobby.queue

import com.example.dobby.dto.RoastResult
import com.example.dobby.util.logger
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class RedisPublisher(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {

    fun publishRoastDelivery(channel: String, request: RoastResult) {
        try {
            val jsonMessage = objectMapper.writeValueAsString(request)
            redisTemplate.convertAndSend(channel, jsonMessage)
        } catch (e: Exception) {
            logger.error("Error publishing message to Redis channel '$channel': ${e.message}")
        }
    }
}