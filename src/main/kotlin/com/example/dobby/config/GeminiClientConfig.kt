package com.example.dobby.config

import com.example.dobby.AppProperties
import com.google.genai.Client
import com.google.genai.types.HttpOptions
import com.google.genai.types.HttpRetryOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GeminiClientConfig(
    private val appProperties: AppProperties,
) {
    @Bean
    fun getClient(): Client {
        val apiKey = appProperties.gemini.apiKey

        val retryOptions = HttpRetryOptions.builder()
            .attempts(3)
            .httpStatusCodes(408, 429)
            .build()
        val httpOptions = HttpOptions.builder()
            .retryOptions(retryOptions)
            .build()
        return Client.builder()
            .apiKey(apiKey)
            .httpOptions(httpOptions)
            .build()
    }
}