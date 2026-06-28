package com.example.dobby.config

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig as KtorClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestRetryConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HttpClientConfig(
    private val json: Json,
) {
    @Bean
    fun httpClient(): HttpClient =
        HttpClient(CIO) {
            configureDobbyJson(json)
            install(HttpRequestRetry) {
                applySharedRetrySettings(this)
            }
        }
}

fun KtorClientConfig<*>.configureDobbyJson(json: Json) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(json)
    }
}

fun applySharedRetrySettings(config: HttpRequestRetryConfig) {
    val logger = LoggerFactory.getLogger("HttpClientRetry")

    config.apply {
        maxRetries = 3
        exponentialDelay(baseDelayMs = 1000, maxDelayMs = 6000)
        retryOnException(maxRetries = 3, retryOnTimeout = true)

        retryIf { request, response ->
            val shouldRetry =
                when (response.status) {
                    HttpStatusCode.TooManyRequests,
                    HttpStatusCode.RequestTimeout,
                    HttpStatusCode.InternalServerError,
                    HttpStatusCode.BadGateway,
                    HttpStatusCode.ServiceUnavailable,
                    -> true

                    else -> false
                }

            if (shouldRetry) {
                logger.warn("HTTP Request to ${request.url} failed with status ${response.status.value}. Retrying...")
            }

            shouldRetry
        }
    }
}
