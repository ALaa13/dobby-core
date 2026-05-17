package com.example.dobby.config

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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