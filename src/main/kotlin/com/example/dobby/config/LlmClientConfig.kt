package com.example.dobby.config

import com.example.dobby.AppProperties
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LlmClientConfig(
    private val appProperties: AppProperties,
) {
    @Bean
    fun openAIClient(): OpenAIClient =
        OpenAIOkHttpClient
            .builder()
            .apiKey(appProperties.llm.apiKey)
            .build()
}
