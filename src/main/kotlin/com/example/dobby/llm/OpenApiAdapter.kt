package com.example.dobby.llm

import com.openai.client.OpenAIClient
import com.openai.models.responses.ResponseCreateParams
import org.springframework.stereotype.Component
import kotlin.streams.asSequence

@Component
class OpenApiAdapter(
    private val client: OpenAIClient,
) : LlmApiPort {
    override suspend fun generate(
        model: String,
        prompt: String,
    ): String {
        val params =
            ResponseCreateParams
                .builder()
                .model(model)
                .input(prompt)
                .build()

        // The official SDK uses client.responses().create()
        val response = client.responses().create(params)

        // Extract and return the generated text
        return response
            .output()
            .stream()
            .flatMap { item -> item.message().stream() }
            .flatMap { message -> message.content().stream() }
            .flatMap { content -> content.outputText().stream() }
            .asSequence()
            .joinToString(separator = "") { outputText -> outputText.text() }
    }
}
