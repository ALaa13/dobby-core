package com.example.dobby.llm

import com.google.genai.Pager
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Model

interface GeminiApiPort {
    suspend fun generateContent(
        model: String,
        prompt: String
    ): GenerateContentResponse

    fun listModels(): Pager<Model>
}