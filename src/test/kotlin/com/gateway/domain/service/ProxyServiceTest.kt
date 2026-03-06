package com.gateway.domain.service

import com.gateway.api.dto.request.ChatCompletionRequest
import com.gateway.api.dto.request.Message
import com.gateway.api.dto.response.ChatCompletionResponse
import com.gateway.api.dto.response.Choice
import com.gateway.api.dto.response.TokenUsage
import com.gateway.api.exception.ApiException
import com.gateway.domain.model.ApiKey
import com.gateway.domain.model.LlmProvider
import com.gateway.domain.model.ModelConfig
import com.gateway.domain.model.Permission
import com.gateway.domain.model.RequestLog
import com.gateway.domain.model.RequestStatus
import com.gateway.infrastructure.llm.LlmClient
import com.gateway.infrastructure.llm.LlmClientFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class ProxyServiceTest {

    private lateinit var llmClientFactory: LlmClientFactory
    private lateinit var modelConfigService: ModelConfigService
    private lateinit var requestLogService: RequestLogService
    private lateinit var proxyService: ProxyService

    private lateinit var mockLlmClient: LlmClient

    private val defaultModelConfig = ModelConfig(
        id = "config-001",
        modelAlias = "gpt-4o",
        provider = LlmProvider.OPENAI,
        modelName = "gpt-4o",
        endpoint = "https://api.openai.com",
        apiKeyEnvVar = "OPENAI_API_KEY",
        costPerInputToken = 0.000005,
        costPerOutputToken = 0.000015,
        maxContextTokens = 128_000,
        maxOutputTokens = 4096,
    )

    private val defaultApiKey = ApiKey(
        id = "key-001",
        key = "gw-test-key-12345",
        name = "Test API Key",
        teamId = "team-001",
        projectId = "project-001",
        permissions = setOf(Permission.CHAT),
        allowedModels = emptySet(),
        enabled = true,
    )

    private val defaultRequest = ChatCompletionRequest(
        model = "gpt-4o",
        messages = listOf(
            Message(role = "user", content = "Hello, how are you?"),
        ),
    )

    private val defaultResponse = ChatCompletionResponse(
        id = "chatcmpl-abc123",
        created = 1709654400L,
        model = "gpt-4o",
        choices = listOf(
            Choice(
                index = 0,
                message = Message(role = "assistant", content = "I'm doing well, thank you!"),
                finishReason = "stop",
            ),
        ),
        usage = TokenUsage(
            promptTokens = 12,
            completionTokens = 8,
            totalTokens = 20,
        ),
    )

    @BeforeEach
    fun setUp() {
        llmClientFactory = mockk()
        modelConfigService = mockk()
        requestLogService = mockk(relaxed = true)
        proxyService = ProxyService(llmClientFactory, modelConfigService, requestLogService)

        mockLlmClient = mockk()
        every { mockLlmClient.provider } returns "openai"
    }

    @Nested
    @DisplayName("chatCompletion - 정상 요청")
    inner class SuccessfulRequest {

        @Test
        @DisplayName("정상 요청 시 응답을 반환하고 provider, latencyMs 메타데이터를 주입한다")
        fun `returns response with provider and latencyMs metadata`() = runTest {
            // given
            coEvery { modelConfigService.resolveModel("gpt-4o") } returns defaultModelConfig
            every { modelConfigService.getProviderApiKey(defaultModelConfig) } returns "sk-test-key"
            every { llmClientFactory.getClientByProvider("openai") } returns mockLlmClient
            coEvery { mockLlmClient.chatCompletion(defaultRequest, "sk-test-key") } returns defaultResponse

            // when
            val result = proxyService.chatCompletion(defaultRequest, defaultApiKey)

            // then
            assertNotNull(result)
            assertEquals("openai", result.provider)
            assertNotNull(result.latencyMs)
            assertEquals("gpt-4o", result.model)
            assertEquals(20, result.usage.totalTokens)
        }

        @Test
        @DisplayName("정상 요청 시 SUCCESS 상태로 requestLogService.saveLog()가 호출된다")
        fun `saves SUCCESS log on successful request`() = runTest {
            // given
            val logSlot = slot<RequestLog>()
            coEvery { modelConfigService.resolveModel("gpt-4o") } returns defaultModelConfig
            every { modelConfigService.getProviderApiKey(defaultModelConfig) } returns "sk-test-key"
            every { llmClientFactory.getClientByProvider("openai") } returns mockLlmClient
            coEvery { mockLlmClient.chatCompletion(defaultRequest, "sk-test-key") } returns defaultResponse
            every { requestLogService.saveLog(capture(logSlot)) } returns Unit

            // when
            proxyService.chatCompletion(defaultRequest, defaultApiKey)

            // then
            verify(exactly = 1) { requestLogService.saveLog(any()) }
            val savedLog = logSlot.captured
            assertEquals(RequestStatus.SUCCESS, savedLog.status)
            assertEquals("openai", savedLog.provider)
            assertEquals("gpt-4o", savedLog.model)
            assertEquals("team-001", savedLog.teamId)
            assertEquals(12, savedLog.inputTokens)
            assertEquals(8, savedLog.outputTokens)
            assertEquals(20, savedLog.totalTokens)
        }
    }

    @Nested
    @DisplayName("chatCompletion - 모델 접근 제어")
    inner class ModelAccessControl {

        @Test
        @DisplayName("allowedModels에 없는 모델 요청 시 FORBIDDEN 에러를 던진다")
        fun `throws FORBIDDEN when model is not in allowedModels`() = runTest {
            // given
            val restrictedApiKey = defaultApiKey.copy(
                allowedModels = setOf("gpt-4o-mini", "gpt-3.5-turbo"),
            )
            val request = defaultRequest.copy(model = "gpt-4o")

            // when & then
            val exception = assertThrows<ApiException> {
                proxyService.chatCompletion(request, restrictedApiKey)
            }

            assertEquals(HttpStatus.FORBIDDEN, exception.status)
            assertEquals("model_not_allowed", exception.errorCode)
        }

        @Test
        @DisplayName("allowedModels가 비어있으면 모든 모델을 허용한다")
        fun `allows all models when allowedModels is empty`() = runTest {
            // given
            val unrestrictedApiKey = defaultApiKey.copy(allowedModels = emptySet())
            coEvery { modelConfigService.resolveModel("gpt-4o") } returns defaultModelConfig
            every { modelConfigService.getProviderApiKey(defaultModelConfig) } returns "sk-test-key"
            every { llmClientFactory.getClientByProvider("openai") } returns mockLlmClient
            coEvery { mockLlmClient.chatCompletion(defaultRequest, "sk-test-key") } returns defaultResponse

            // when
            val result = proxyService.chatCompletion(defaultRequest, unrestrictedApiKey)

            // then
            assertNotNull(result)
            assertEquals("gpt-4o", result.model)
        }

        @Test
        @DisplayName("allowedModels에 포함된 모델 요청은 정상 처리된다")
        fun `allows request when model is in allowedModels`() = runTest {
            // given
            val restrictedApiKey = defaultApiKey.copy(
                allowedModels = setOf("gpt-4o", "gpt-4o-mini"),
            )
            coEvery { modelConfigService.resolveModel("gpt-4o") } returns defaultModelConfig
            every { modelConfigService.getProviderApiKey(defaultModelConfig) } returns "sk-test-key"
            every { llmClientFactory.getClientByProvider("openai") } returns mockLlmClient
            coEvery { mockLlmClient.chatCompletion(defaultRequest, "sk-test-key") } returns defaultResponse

            // when
            val result = proxyService.chatCompletion(defaultRequest, restrictedApiKey)

            // then
            assertNotNull(result)
            assertEquals("gpt-4o", result.model)
        }
    }

    @Nested
    @DisplayName("chatCompletion - 에러 처리")
    inner class ErrorHandling {

        @Test
        @DisplayName("LLM client에서 ApiException 발생 시 FAILED 로그를 저장하고 재전파한다")
        fun `saves FAILED log and rethrows ApiException from LLM client`() = runTest {
            // given
            val apiException = ApiException(
                status = HttpStatus.TOO_MANY_REQUESTS,
                message = "Rate limit exceeded",
                errorCode = "rate_limit_exceeded",
            )
            val logSlot = slot<RequestLog>()
            coEvery { modelConfigService.resolveModel("gpt-4o") } returns defaultModelConfig
            every { modelConfigService.getProviderApiKey(defaultModelConfig) } returns "sk-test-key"
            every { llmClientFactory.getClientByProvider("openai") } returns mockLlmClient
            coEvery { mockLlmClient.chatCompletion(defaultRequest, "sk-test-key") } throws apiException
            every { requestLogService.saveLog(capture(logSlot)) } returns Unit

            // when & then
            val thrown = assertThrows<ApiException> {
                proxyService.chatCompletion(defaultRequest, defaultApiKey)
            }

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, thrown.status)
            assertEquals("rate_limit_exceeded", thrown.errorCode)

            verify(exactly = 1) { requestLogService.saveLog(any()) }
            val savedLog = logSlot.captured
            assertEquals(RequestStatus.FAILED, savedLog.status)
            assertEquals("rate_limit_exceeded", savedLog.errorCode)
            assertEquals("Rate limit exceeded", savedLog.errorMessage)
        }

        @Test
        @DisplayName("LLM client에서 일반 Exception 발생 시 INTERNAL_SERVER_ERROR로 변환한다")
        fun `converts generic Exception to INTERNAL_SERVER_ERROR`() = runTest {
            // given
            val logSlot = slot<RequestLog>()
            coEvery { modelConfigService.resolveModel("gpt-4o") } returns defaultModelConfig
            every { modelConfigService.getProviderApiKey(defaultModelConfig) } returns "sk-test-key"
            every { llmClientFactory.getClientByProvider("openai") } returns mockLlmClient
            coEvery {
                mockLlmClient.chatCompletion(defaultRequest, "sk-test-key")
            } throws RuntimeException("Connection timeout")
            every { requestLogService.saveLog(capture(logSlot)) } returns Unit

            // when & then
            val thrown = assertThrows<ApiException> {
                proxyService.chatCompletion(defaultRequest, defaultApiKey)
            }

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, thrown.status)
            assertEquals("internal_error", thrown.errorCode)

            verify(exactly = 1) { requestLogService.saveLog(any()) }
            val savedLog = logSlot.captured
            assertEquals(RequestStatus.FAILED, savedLog.status)
            assertEquals("internal_error", savedLog.errorCode)
            assertEquals("Connection timeout", savedLog.errorMessage)
        }

        @Test
        @DisplayName("모든 에러 케이스에서 requestLogService.saveLog()가 호출된다")
        fun `always calls saveLog even on errors`() = runTest {
            // given - ApiException case
            coEvery { modelConfigService.resolveModel("gpt-4o") } returns defaultModelConfig
            every { modelConfigService.getProviderApiKey(defaultModelConfig) } returns "sk-test-key"
            every { llmClientFactory.getClientByProvider("openai") } returns mockLlmClient
            coEvery {
                mockLlmClient.chatCompletion(defaultRequest, "sk-test-key")
            } throws ApiException(
                status = HttpStatus.BAD_GATEWAY,
                message = "Provider unavailable",
                errorCode = "provider_unavailable",
            )

            // when & then
            assertThrows<ApiException> {
                proxyService.chatCompletion(defaultRequest, defaultApiKey)
            }

            verify(exactly = 1) { requestLogService.saveLog(any()) }
        }
    }

    @Nested
    @DisplayName("chatCompletion - 비용 계산")
    inner class CostCalculation {

        @Test
        @DisplayName("calculateCost가 costPerInputToken * inputTokens + costPerOutputToken * outputTokens를 정확히 계산한다")
        fun `calculates cost correctly based on token usage and model pricing`() = runTest {
            // given
            val modelConfig = defaultModelConfig.copy(
                costPerInputToken = 0.000005,  // $5 per 1M input tokens
                costPerOutputToken = 0.000015,  // $15 per 1M output tokens
            )
            val response = defaultResponse.copy(
                usage = TokenUsage(
                    promptTokens = 1000,
                    completionTokens = 500,
                    totalTokens = 1500,
                ),
            )
            val logSlot = slot<RequestLog>()

            coEvery { modelConfigService.resolveModel("gpt-4o") } returns modelConfig
            every { modelConfigService.getProviderApiKey(modelConfig) } returns "sk-test-key"
            every { llmClientFactory.getClientByProvider("openai") } returns mockLlmClient
            coEvery { mockLlmClient.chatCompletion(defaultRequest, "sk-test-key") } returns response
            every { requestLogService.saveLog(capture(logSlot)) } returns Unit

            // when
            proxyService.chatCompletion(defaultRequest, defaultApiKey)

            // then
            verify(exactly = 1) { requestLogService.saveLog(any()) }
            val savedLog = logSlot.captured

            // Expected cost: (0.000005 * 1000) + (0.000015 * 500) = 0.005 + 0.0075 = 0.0125
            val expectedCost = (0.000005 * 1000) + (0.000015 * 500)
            assertEquals(expectedCost, savedLog.costUsd, 0.0000001)
        }

        @Test
        @DisplayName("토큰이 0인 경우 비용도 0이다")
        fun `calculates zero cost when tokens are zero`() = runTest {
            // given
            val modelConfig = defaultModelConfig.copy(
                costPerInputToken = 0.000005,
                costPerOutputToken = 0.000015,
            )
            val response = defaultResponse.copy(
                usage = TokenUsage(
                    promptTokens = 0,
                    completionTokens = 0,
                    totalTokens = 0,
                ),
            )
            val logSlot = slot<RequestLog>()

            coEvery { modelConfigService.resolveModel("gpt-4o") } returns modelConfig
            every { modelConfigService.getProviderApiKey(modelConfig) } returns "sk-test-key"
            every { llmClientFactory.getClientByProvider("openai") } returns mockLlmClient
            coEvery { mockLlmClient.chatCompletion(defaultRequest, "sk-test-key") } returns response
            every { requestLogService.saveLog(capture(logSlot)) } returns Unit

            // when
            proxyService.chatCompletion(defaultRequest, defaultApiKey)

            // then
            val savedLog = logSlot.captured
            assertEquals(0.0, savedLog.costUsd, 0.0000001)
        }
    }

    @Nested
    @DisplayName("chatCompletion - Anthropic provider 연동")
    inner class AnthropicProviderIntegration {

        @Test
        @DisplayName("Anthropic 모델 요청 시 올바른 provider client가 사용된다")
        fun `uses anthropic client for claude models`() = runTest {
            // given
            val anthropicConfig = ModelConfig(
                modelAlias = "claude-sonnet-4-20250514",
                provider = LlmProvider.ANTHROPIC,
                modelName = "claude-sonnet-4-20250514",
                endpoint = "https://api.anthropic.com",
                apiKeyEnvVar = "ANTHROPIC_API_KEY",
                costPerInputToken = 0.000003,
                costPerOutputToken = 0.000015,
                maxContextTokens = 200_000,
            )
            val anthropicClient: LlmClient = mockk()
            every { anthropicClient.provider } returns "anthropic"

            val anthropicRequest = defaultRequest.copy(model = "claude-sonnet-4-20250514")
            val anthropicResponse = defaultResponse.copy(
                model = "claude-sonnet-4-20250514",
            )
            val apiKeyForClaude = defaultApiKey.copy(allowedModels = emptySet())

            coEvery { modelConfigService.resolveModel("claude-sonnet-4-20250514") } returns anthropicConfig
            every { modelConfigService.getProviderApiKey(anthropicConfig) } returns "sk-ant-test-key"
            every { llmClientFactory.getClientByProvider("anthropic") } returns anthropicClient
            coEvery {
                anthropicClient.chatCompletion(anthropicRequest, "sk-ant-test-key")
            } returns anthropicResponse

            // when
            val result = proxyService.chatCompletion(anthropicRequest, apiKeyForClaude)

            // then
            assertEquals("anthropic", result.provider)
            assertEquals("claude-sonnet-4-20250514", result.model)
        }
    }
}
