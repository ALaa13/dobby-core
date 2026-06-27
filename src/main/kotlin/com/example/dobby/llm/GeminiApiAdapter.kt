package com.example.dobby.llm

import com.google.genai.Client
import com.google.genai.Pager
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Model
import org.springframework.stereotype.Component

@Component
class GeminiApiAdapter(
    private val client: Client,
) : GeminiApiPort {
    override fun listModels(): Pager<Model> = client.models.list(null)

    override suspend fun generateContent(
        model: String,
        prompt: String,
        config: GenerateContentConfig?,
    ): GenerateContentResponse = client.models.generateContent(model, prompt, null)
}
