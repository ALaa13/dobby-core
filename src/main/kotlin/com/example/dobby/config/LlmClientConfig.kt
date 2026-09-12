package com.example.dobby.config

import com.example.dobby.AppProperties
import com.openai.client.OpenAIClientAsync
import com.openai.client.okhttp.OpenAIOkHttpClientAsync
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LlmClientConfig(
    private val appProperties: AppProperties,
) {
    @Bean
    fun openAIClient(): OpenAIClientAsync {
        // Provider construction stays at the infrastructure edge so application services only see LlmApiPort.
        return OpenAIOkHttpClientAsync
            .builder()
            .apiKey(appProperties.llm.apiKey)
            .build()
    }
}
