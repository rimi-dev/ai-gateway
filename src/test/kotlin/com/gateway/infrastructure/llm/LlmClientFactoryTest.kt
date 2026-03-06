package com.gateway.infrastructure.llm

import com.gateway.api.dto.request.ChatCompletionRequest
import com.gateway.api.dto.response.ChatCompletionChunkResponse
import com.gateway.api.dto.response.ChatCompletionResponse
import com.gateway.api.exception.ApiException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class LlmClientFactoryTest {

    private fun createMockClient(providerName: String, supportedModels: Set<String>): LlmClient {
        return mockk<LlmClient> {
            every { provider } returns providerName
            every { supports(any()) } answers { firstArg<String>() in supportedModels }
        }
    }

    @Nested
    @DisplayName("getClient")
    inner class GetClient {

        @Test
        @DisplayName("지원하는 모델이 있으면 해당 클라이언트를 반환한다")
        fun `returns matching client for supported model`() {
            val openAiClient = createMockClient("openai", setOf("gpt-4", "gpt-4o"))
            val claudeClient = createMockClient("anthropic", setOf("claude-3-opus", "claude-3-sonnet"))
            val factory = LlmClientFactory(listOf(openAiClient, claudeClient))

            val result = factory.getClient("gpt-4")

            assertSame(openAiClient, result)
        }

        @Test
        @DisplayName("두 번째 클라이언트가 모델을 지원하면 해당 클라이언트를 반환한다")
        fun `returns second client when it supports the model`() {
            val openAiClient = createMockClient("openai", setOf("gpt-4", "gpt-4o"))
            val claudeClient = createMockClient("anthropic", setOf("claude-3-opus", "claude-3-sonnet"))
            val factory = LlmClientFactory(listOf(openAiClient, claudeClient))

            val result = factory.getClient("claude-3-opus")

            assertSame(claudeClient, result)
        }

        @Test
        @DisplayName("매칭되는 클라이언트가 없으면 ApiException을 던진다")
        fun `throws ApiException when no client supports the model`() {
            val openAiClient = createMockClient("openai", setOf("gpt-4"))
            val factory = LlmClientFactory(listOf(openAiClient))

            val exception = assertThrows<ApiException> {
                factory.getClient("unknown-model")
            }

            assertEquals(HttpStatus.BAD_REQUEST, exception.status)
            assertEquals("model_not_supported", exception.errorCode)
            assertEquals("No LLM client found for model: unknown-model", exception.message)
        }

        @Test
        @DisplayName("클라이언트 목록이 비어있으면 ApiException을 던진다")
        fun `throws ApiException when client list is empty`() {
            val factory = LlmClientFactory(emptyList())

            val exception = assertThrows<ApiException> {
                factory.getClient("gpt-4")
            }

            assertEquals(HttpStatus.BAD_REQUEST, exception.status)
            assertEquals("model_not_supported", exception.errorCode)
        }
    }

    @Nested
    @DisplayName("getClientByProvider")
    inner class GetClientByProvider {

        @Test
        @DisplayName("provider 이름으로 클라이언트를 반환한다")
        fun `returns client matching provider name`() {
            val openAiClient = createMockClient("openai", emptySet())
            val claudeClient = createMockClient("anthropic", emptySet())
            val factory = LlmClientFactory(listOf(openAiClient, claudeClient))

            val result = factory.getClientByProvider("openai")

            assertSame(openAiClient, result)
        }

        @Test
        @DisplayName("provider 이름 대소문자를 무시하고 클라이언트를 반환한다")
        fun `returns client with case-insensitive provider match`() {
            val openAiClient = createMockClient("openai", emptySet())
            val factory = LlmClientFactory(listOf(openAiClient))

            val result = factory.getClientByProvider("OpenAI")

            assertSame(openAiClient, result)
        }

        @Test
        @DisplayName("대문자 provider 이름으로도 매칭된다")
        fun `returns client with uppercase provider name`() {
            val claudeClient = createMockClient("anthropic", emptySet())
            val factory = LlmClientFactory(listOf(claudeClient))

            val result = factory.getClientByProvider("ANTHROPIC")

            assertSame(claudeClient, result)
        }

        @Test
        @DisplayName("매칭되는 provider가 없으면 ApiException을 던진다")
        fun `throws ApiException when no client matches provider`() {
            val openAiClient = createMockClient("openai", emptySet())
            val factory = LlmClientFactory(listOf(openAiClient))

            val exception = assertThrows<ApiException> {
                factory.getClientByProvider("unknown-provider")
            }

            assertEquals(HttpStatus.BAD_REQUEST, exception.status)
            assertEquals("provider_not_supported", exception.errorCode)
            assertEquals("No LLM client found for provider: unknown-provider", exception.message)
        }

        @Test
        @DisplayName("클라이언트 목록이 비어있으면 ApiException을 던진다")
        fun `throws ApiException when client list is empty`() {
            val factory = LlmClientFactory(emptyList())

            val exception = assertThrows<ApiException> {
                factory.getClientByProvider("openai")
            }

            assertEquals(HttpStatus.BAD_REQUEST, exception.status)
            assertEquals("provider_not_supported", exception.errorCode)
        }
    }
}
