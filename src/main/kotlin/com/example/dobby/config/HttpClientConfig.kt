package com.example.dobby.config

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.ktor.client.HttpClientConfig as KtorClientConfig

@Configuration
class HttpClientConfig {
    @Bean
    fun httpClient(): HttpClient {
        return HttpClient(CIO) {
            configureDobbyJson()
        }
    }
}


fun KtorClientConfig<*>.configureDobbyJson() {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = false
        })
    }
}