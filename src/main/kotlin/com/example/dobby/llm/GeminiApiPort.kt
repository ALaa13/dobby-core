package com.example.dobby.llm

import com.google.genai.Pager
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Model

interface GeminiApiPort {
    suspend fun generateContent(
        model: String,
        prompt: String,
        config: GenerateContentConfig? = null,
    ): GenerateContentResponse

    fun listModels(): Pager<Model>
}
