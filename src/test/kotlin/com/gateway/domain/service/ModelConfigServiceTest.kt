package com.gateway.domain.service

import com.gateway.api.exception.ApiException
import com.gateway.config.properties.LlmProperties
import com.gateway.config.properties.ProviderConfig
import com.gateway.domain.model.LlmProvider
import com.gateway.domain.model.ModelConfig
import com.gateway.domain.repository.ModelConfigRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class ModelConfigServiceTest {

    private lateinit var modelConfigRepository: ModelConfigRepository
    private lateinit var llmProperties: LlmProperties
    private lateinit var modelConfigService: ModelConfigService

    @BeforeEach
    fun setUp() {
        modelConfigRepository = mockk()
        llmProperties = LlmProperties(
            providers = mapOf(
                "claude" to ProviderConfig(
                    baseUrl = "https://api.anthropic.com",
                    defaultModel = "claude-sonnet-4-20250514",
                ),
                "openai" to ProviderConfig(
                    baseUrl = "https://api.openai.com",
                    defaultModel = "gpt-4o",
                ),
                "gemini" to ProviderConfig(
                    baseUrl = "https://generativelanguage.googleapis.com",
                    defaultModel = "gemini-pro",
                ),
            ),
        )
        modelConfigService = ModelConfigService(modelConfigRepository, llmProperties)
    }

    @Nested
    @DisplayName("resolveModel")
    inner class ResolveModel {

        @Test
        @DisplayName("DB에서 enabled 모델을 찾으면 해당 config를 반환한다")
        fun `returns DB config when model is found and enabled`() = runTest {
            // given
            val modelAlias = "my-custom-claude"
            val dbConfig = ModelConfig(
                id = "config-001",
                modelAlias = modelAlias,
                provider = LlmProvider.ANTHROPIC,
                modelName = "claude-sonnet-4-20250514",
                endpoint = "https://api.anthropic.com",
                apiKeyEnvVar = "ANTHROPIC_API_KEY",
                costPerInputToken = 0.003,
                costPerOutputToken = 0.015,
                maxContextTokens = 200_000,
                maxOutputTokens = 8192,
                enabled = true,
            )
            coEvery { modelConfigRepository.findByModelAlias(modelAlias) } returns dbConfig

            // when
            val result = modelConfigService.resolveModel(modelAlias)

            // then
            assertEquals(dbConfig, result)
            assertEquals(LlmProvider.ANTHROPIC, result.provider)
            assertEquals("claude-sonnet-4-20250514", result.modelName)
            assertEquals(true, result.enabled)
        }

        @Test
        @DisplayName("DB에서 찾았지만 disabled이면 기본 매핑을 사용한다")
        fun `falls back to default mappings when DB config is disabled`() = runTest {
            // given
            val modelAlias = "gpt-4o"
            val disabledConfig = ModelConfig(
                id = "config-002",
                modelAlias = modelAlias,
                provider = LlmProvider.OPENAI,
                modelName = "gpt-4o",
                endpoint = "https://api.openai.com",
                apiKeyEnvVar = "OPENAI_API_KEY",
                costPerInputToken = 0.005,
                costPerOutputToken = 0.015,
                maxContextTokens = 128_000,
                enabled = false,
            )
            coEvery { modelConfigRepository.findByModelAlias(modelAlias) } returns disabledConfig

            // when
            val result = modelConfigService.resolveModel(modelAlias)

            // then
            assertEquals(LlmProvider.OPENAI, result.provider)
            assertEquals(modelAlias, result.modelAlias)
            assertEquals(modelAlias, result.modelName)
        }

        @Test
        @DisplayName("DB에 없으면 DEFAULT_MODEL_MAPPINGS에서 찾는다")
        fun `falls back to DEFAULT_MODEL_MAPPINGS when not found in DB`() = runTest {
            // given
            val modelAlias = "gpt-4o"
            coEvery { modelConfigRepository.findByModelAlias(modelAlias) } returns null

            // when
            val result = modelConfigService.resolveModel(modelAlias)

            // then
            assertEquals(LlmProvider.OPENAI, result.provider)
            assertEquals(modelAlias, result.modelAlias)
            assertEquals(modelAlias, result.modelName)
            assertEquals("OPENAI_API_KEY", result.apiKeyEnvVar)
        }

        @Test
        @DisplayName("DEFAULT_MODEL_MAPPINGS에도 없으면 모델명으로 provider를 추론한다")
        fun `infers provider from model name prefix when not in default mappings`() = runTest {
            // given
            val modelAlias = "claude-new-model-2026"
            coEvery { modelConfigRepository.findByModelAlias(modelAlias) } returns null

            // when
            val result = modelConfigService.resolveModel(modelAlias)

            // then
            assertEquals(LlmProvider.ANTHROPIC, result.provider)
            assertEquals(modelAlias, result.modelAlias)
            assertEquals(modelAlias, result.modelName)
        }

        @Test
        @DisplayName("gemini 접두사로 GOOGLE provider를 추론한다")
        fun `infers GOOGLE provider from gemini prefix`() = runTest {
            // given
            val modelAlias = "gemini-2.0-flash"
            coEvery { modelConfigRepository.findByModelAlias(modelAlias) } returns null

            // when
            val result = modelConfigService.resolveModel(modelAlias)

            // then
            assertEquals(LlmProvider.GOOGLE, result.provider)
        }

        @Test
        @DisplayName("o4 접두사로 OPENAI provider를 추론한다")
        fun `infers OPENAI provider from o4 prefix`() = runTest {
            // given
            val modelAlias = "o4-mini"
            coEvery { modelConfigRepository.findByModelAlias(modelAlias) } returns null

            // when
            val result = modelConfigService.resolveModel(modelAlias)

            // then
            assertEquals(LlmProvider.OPENAI, result.provider)
        }

        @Test
        @DisplayName("provider 추론 실패 시 BAD_REQUEST ApiException을 던진다")
        fun `throws BAD_REQUEST ApiException when provider inference fails`() = runTest {
            // given
            val modelAlias = "unknown-model-xyz"
            coEvery { modelConfigRepository.findByModelAlias(modelAlias) } returns null

            // when & then
            val exception = assertThrows<ApiException> {
                modelConfigService.resolveModel(modelAlias)
            }

            assertEquals(HttpStatus.BAD_REQUEST, exception.status)
            assertEquals("model_not_found", exception.errorCode)
        }
    }

    @Nested
    @DisplayName("getProviderApiKey")
    inner class GetProviderApiKey {

        private val testEnvVarKey = "TEST_GATEWAY_API_KEY_FOR_UNIT_TEST"

        /**
         * Modifies the in-process environment variable map via reflection.
         * This works on JDK implementations where ProcessEnvironment holds an internal map.
         */
        @Suppress("UNCHECKED_CAST")
        private fun setEnv(key: String, value: String) {
            val processEnvironment = Class.forName("java.lang.ProcessEnvironment")
            val envField = processEnvironment.getDeclaredField("theEnvironment")
            envField.isAccessible = true
            val env = envField.get(null) as MutableMap<Any, Any>

            // ProcessEnvironment uses internal Variable/Value types on some JDKs,
            // try direct string approach first
            try {
                val variableClass = Class.forName("java.lang.ProcessEnvironment\$Variable")
                val valueClass = Class.forName("java.lang.ProcessEnvironment\$Value")
                val variableOf = variableClass.getDeclaredMethod("valueOf", ByteArray::class.java)
                variableOf.isAccessible = true
                val valueOf = valueClass.getDeclaredMethod("valueOf", ByteArray::class.java)
                valueOf.isAccessible = true
                env[variableOf.invoke(null, key.toByteArray())] = valueOf.invoke(null, value.toByteArray())
            } catch (e: ClassNotFoundException) {
                // Fallback for simpler JDK implementations
                env[key] = value
            }

            // Also update the unmodifiable environment map used by System.getenv()
            val unmodifiableEnvField = processEnvironment.getDeclaredField("theUnmodifiableEnvironment")
            unmodifiableEnvField.isAccessible = true
            val unmodifiableEnv = unmodifiableEnvField.get(null) as Map<String, String>

            // The unmodifiable map wraps an internal map; update it via its backing map
            val unmodifiableMapClass = unmodifiableEnv.javaClass
            val backingMapField = unmodifiableMapClass.getDeclaredField("m")
            backingMapField.isAccessible = true
            val backingMap = backingMapField.get(unmodifiableEnv) as MutableMap<String, String>
            backingMap[key] = value
        }

        @Suppress("UNCHECKED_CAST")
        private fun removeEnv(key: String) {
            val processEnvironment = Class.forName("java.lang.ProcessEnvironment")
            val envField = processEnvironment.getDeclaredField("theEnvironment")
            envField.isAccessible = true
            val env = envField.get(null) as MutableMap<Any, Any>

            try {
                val variableClass = Class.forName("java.lang.ProcessEnvironment\$Variable")
                val variableOf = variableClass.getDeclaredMethod("valueOf", ByteArray::class.java)
                variableOf.isAccessible = true
                env.remove(variableOf.invoke(null, key.toByteArray()))
            } catch (e: ClassNotFoundException) {
                env.remove(key)
            }

            val unmodifiableEnvField = processEnvironment.getDeclaredField("theUnmodifiableEnvironment")
            unmodifiableEnvField.isAccessible = true
            val unmodifiableEnv = unmodifiableEnvField.get(null) as Map<String, String>
            val unmodifiableMapClass = unmodifiableEnv.javaClass
            val backingMapField = unmodifiableMapClass.getDeclaredField("m")
            backingMapField.isAccessible = true
            val backingMap = backingMapField.get(unmodifiableEnv) as MutableMap<String, String>
            backingMap.remove(key)
        }

        @AfterEach
        fun cleanUpEnv() {
            removeEnv(testEnvVarKey)
        }

        @Test
        @DisplayName("환경변수가 설정된 경우 값을 반환한다")
        fun `returns API key when environment variable is set`() {
            // given
            setEnv(testEnvVarKey, "sk-test-api-key-12345")
            val config = ModelConfig(
                modelAlias = "gpt-4o",
                provider = LlmProvider.OPENAI,
                modelName = "gpt-4o",
                endpoint = "https://api.openai.com",
                apiKeyEnvVar = testEnvVarKey,
                costPerInputToken = 0.005,
                costPerOutputToken = 0.015,
                maxContextTokens = 128_000,
            )

            // when
            val result = modelConfigService.getProviderApiKey(config)

            // then
            assertEquals("sk-test-api-key-12345", result)
        }

        @Test
        @DisplayName("apiKeyEnvVar가 비어있으면 PROVIDER_API_KEY_ENV_VARS에서 조회한다")
        fun `looks up env var from PROVIDER_API_KEY_ENV_VARS when apiKeyEnvVar is empty`() {
            // given - Set the ANTHROPIC_API_KEY env var used by PROVIDER_API_KEY_ENV_VARS
            setEnv("ANTHROPIC_API_KEY", "sk-ant-test-key")
            val config = ModelConfig(
                modelAlias = "claude-sonnet-4-20250514",
                provider = LlmProvider.ANTHROPIC,
                modelName = "claude-sonnet-4-20250514",
                endpoint = "https://api.anthropic.com",
                apiKeyEnvVar = "",
                costPerInputToken = 0.003,
                costPerOutputToken = 0.015,
                maxContextTokens = 200_000,
            )

            // when
            val result = modelConfigService.getProviderApiKey(config)

            // then
            assertEquals("sk-ant-test-key", result)

            // cleanup
            removeEnv("ANTHROPIC_API_KEY")
        }

        @Test
        @DisplayName("환경변수가 미설정 시 INTERNAL_SERVER_ERROR ApiException을 던진다")
        fun `throws INTERNAL_SERVER_ERROR ApiException when env var is not set`() {
            // given - Use an env var name that definitely doesn't exist
            val config = ModelConfig(
                modelAlias = "gpt-4o",
                provider = LlmProvider.OPENAI,
                modelName = "gpt-4o",
                endpoint = "https://api.openai.com",
                apiKeyEnvVar = "NONEXISTENT_API_KEY_FOR_TEST_XYZ_999",
                costPerInputToken = 0.005,
                costPerOutputToken = 0.015,
                maxContextTokens = 128_000,
            )

            // when & then
            val exception = assertThrows<ApiException> {
                modelConfigService.getProviderApiKey(config)
            }

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.status)
            assertEquals("missing_api_key", exception.errorCode)
        }
    }
}
