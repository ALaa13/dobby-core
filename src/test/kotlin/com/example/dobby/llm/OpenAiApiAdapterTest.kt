package com.example.dobby.llm

import com.openai.client.OpenAIClientAsync
import com.openai.models.responses.Response
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.ResponseOutputItem
import com.openai.models.responses.ResponseOutputMessage
import com.openai.models.responses.ResponseOutputText
import com.openai.services.async.ResponseServiceAsync
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals

class OpenAiApiAdapterTest {
    private val client = mockk<OpenAIClientAsync>()
    private val responses = mockk<ResponseServiceAsync>()
    private val adapter = OpenAiApiAdapter(client)

    @Test
    fun `generate awaits the asynchronous response and returns all output text`() =
        runTest {
            val params = slot<ResponseCreateParams>()
            val response = mockk<Response>()
            val firstItem = mockk<ResponseOutputItem>()
            val secondItem = mockk<ResponseOutputItem>()
            val firstMessage = mockk<ResponseOutputMessage>()
            val secondMessage = mockk<ResponseOutputMessage>()
            val firstContent = mockk<ResponseOutputMessage.Content>()
            val secondContent = mockk<ResponseOutputMessage.Content>()
            val firstText = mockk<ResponseOutputText>()
            val secondText = mockk<ResponseOutputText>()

            every { client.responses() } returns responses
            every { responses.create(capture(params)) } returns CompletableFuture.completedFuture(response)
            every { response.output() } returns listOf(firstItem, secondItem)
            every { firstItem.message() } returns Optional.of(firstMessage)
            every { secondItem.message() } returns Optional.of(secondMessage)
            every { firstMessage.content() } returns listOf(firstContent)
            every { secondMessage.content() } returns listOf(secondContent)
            every { firstContent.outputText() } returns Optional.of(firstText)
            every { secondContent.outputText() } returns Optional.of(secondText)
            every { firstText.text() } returns "first "
            every { secondText.text() } returns "second"

            val result = adapter.generate("gpt-5", "Write a roast")

            assertEquals("first second", result)
            assertEquals(
                "gpt-5",
                params.captured
                    .model()
                    .get()
                    .asString(),
            )
            assertEquals(
                "Write a roast",
                params.captured
                    .input()
                    .get()
                    .asText(),
            )
        }
}
