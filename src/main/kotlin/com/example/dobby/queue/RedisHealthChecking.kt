package com.example.dobby.queue

import ch.qos.logback.classic.LoggerContext
import com.example.dobby.config.log
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import kotlin.system.exitProcess

@Configuration
@Profile("!test")
class RedisHealthCheckConfig {

    @Bean
    fun checkRedisConnection(connectionFactory: RedisConnectionFactory) = CommandLineRunner {
        try {
            connectionFactory.connection.use { connection ->
                val response = connection.ping()
                if (response == "PONG" || response != null) {
                    log.info("Redis Health Check Passed: Backend connected successfully.")
                } else {
                    throw IllegalStateException("Redis responded, but PING failed.")
                }
            }
        } catch (e: Exception) {
            log.error("CRITICAL ERROR: Redis is offline! The backend application cannot start.")
            log.error("Reason: ${e.message}")
            (LoggerFactory.getILoggerFactory() as? LoggerContext)?.stop()
            exitProcess(1)
        }
    }
}