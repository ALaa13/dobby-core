package com.example.dobby.queue

import ch.qos.logback.classic.LoggerContext
import com.example.dobby.util.logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import kotlin.system.exitProcess

@Configuration
class RedisHealthCheckConfig {

    @Bean
    fun checkRedisConnection(connectionFactory: RedisConnectionFactory) = CommandLineRunner {
        try {
            connectionFactory.connection.use { connection ->
                val response = connection.ping()
                if (response == "PONG" || response != null) {
                    logger.info("Redis Health Check Passed: Backend connected successfully.")
                } else {
                    throw IllegalStateException("Redis responded, but PING failed.")
                }
            }
        } catch (e: Exception) {
            logger.error("CRITICAL ERROR: Redis is offline! The backend application cannot start.")
            logger.error("Reason: ${e.message}")
            (LoggerFactory.getILoggerFactory() as? LoggerContext)?.stop()
            exitProcess(1)
        }
    }
}