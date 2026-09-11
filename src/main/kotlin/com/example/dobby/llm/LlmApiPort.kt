package com.example.dobby.llm

/**
 * Provider-neutral generation boundary; application services must not depend on SDK-specific response types.
 */
interface LlmApiPort {
    suspend fun generate(
        model: String,
        prompt: String,
    ): String
}
