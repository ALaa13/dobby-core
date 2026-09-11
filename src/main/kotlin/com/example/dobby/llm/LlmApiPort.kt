package com.example.dobby.llm

interface LlmApiPort {
    suspend fun generate(
        model: String,
        prompt: String,
    ): String
}
