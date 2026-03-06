package com.gateway.api.proxy

import com.gateway.api.dto.request.ChatCompletionRequest
import com.gateway.api.dto.request.Message
import com.gateway.api.dto.response.ChatCompletionResponse
import com.gateway.api.dto.response.Choice
import com.gateway.api.dto.response.TokenUsage
import com.gateway.api.exception.ApiException
import com.gateway.domain.model.ApiKey
import com.gateway.domain.service.ProxyService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ServerWebExchange

class ProxyControllerTest {

    private val proxyService = mockk<ProxyService>()
    private val controller = ProxyController(proxyService)

    private val testApiKey = ApiKey(
        id = "key-123",
        key = "sk-test-key",
        name = "Test Key",
        teamId = "team-1",
        projectId = "project-1",
    )

    private val testRequest = ChatCompletionRequest(
        model = "gpt-4",
        messages = listOf(
            Message(role = "user", content = "Hello"),
        ),
    )

    private val testResponse = ChatCompletionResponse(
        id = "chatcmpl-123",
        created = 1234567890L,
        model = "gpt-4",
        choices = listOf(
            Choice(
                index = 0,
                message = Message(role = "assistant", content = "Hi there!"),
                finishReason = "stop",
            ),
        ),
        usage = TokenUsage(
            promptTokens = 10,
            completionTokens = 5,
            totalTokens = 15,
        ),
    )

    @Nested
    @DisplayName("chatCompletions")
    inner class ChatCompletions {

        @Test
        @DisplayName("apiKey attribute가 있으면 proxyService를 호출하고 응답을 반환한다")
        fun `calls proxyService and returns response when apiKey is present`() = runTest {
            val exchange = mockk<ServerWebExchange>()
            every { exchange.getAttribute<ApiKey>("apiKey") } returns testApiKey
            coEvery { proxyService.chatCompletion(testRequest, testApiKey) } returns testResponse

            val result = controller.chatCompletions(testRequest, exchange)

            assertSame(testResponse, result)
            assertEquals("chatcmpl-123", result.id)
            assertEquals("gpt-4", result.model)
            coVerify(exactly = 1) { proxyService.chatCompletion(testRequest, testApiKey) }
        }

        @Test
        @DisplayName("apiKey attribute가 없으면 UNAUTHORIZED ApiException을 던진다")
        fun `throws ApiException when apiKey attribute is missing`() = runTest {
            val exchange = mockk<ServerWebExchange>()
            every { exchange.getAttribute<ApiKey>("apiKey") } returns null

            val exception = assertThrows<ApiException> {
                controller.chatCompletions(testRequest, exchange)
            }

            assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
            assertEquals("Authentication required", exception.message)
            assertEquals("unauthenticated", exception.errorCode)
        }
    }
}
