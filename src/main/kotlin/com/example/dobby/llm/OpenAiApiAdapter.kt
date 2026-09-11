package com.example.dobby.llm

import com.openai.client.OpenAIClient
import com.openai.models.responses.ResponseCreateParams
import org.springframework.stereotype.Component
import kotlin.streams.asSequence

@Component
class OpenAiApiAdapter(
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

        // Keep SDK request and response types inside this adapter so callers remain provider-neutral.
        val response = client.responses().create(params)

        // Collapse all text output items into the single String promised by LlmApiPort.
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
