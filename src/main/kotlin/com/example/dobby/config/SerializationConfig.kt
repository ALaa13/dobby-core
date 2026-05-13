package com.example.dobby.config

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * A configuration class for setting up JSON serialization with Kotlinx Serialization.
 *
 * This configuration is applied to ensure proper handling of JSON serialization and deserialization
 * throughout the application. It modifies the behavior to:
 * - Ignore unknown keys present in incoming JSON payloads, preventing errors when extra fields exist.
 * - Exclude null values explicitly from the serialization output.
 *
 * The configured `Json` instance is defined as a Spring Bean to allow its reuse across the application.
 */

@Configuration
class SerializationConfig {
    @Bean
    fun kotlinxSerializationJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}