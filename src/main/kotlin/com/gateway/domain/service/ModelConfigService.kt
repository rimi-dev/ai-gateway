package com.gateway.domain.service

import com.gateway.api.exception.ApiException
import com.gateway.config.properties.LlmProperties
import com.gateway.domain.model.LlmProvider
import com.gateway.domain.model.ModelConfig
import com.gateway.domain.repository.ModelConfigRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ModelConfigService(
    private val modelConfigRepository: ModelConfigRepository,
    private val llmProperties: LlmProperties,
) {

    companion object {
        private val DEFAULT_MODEL_MAPPINGS = mapOf(
            "claude-sonnet-4-20250514" to LlmProvider.ANTHROPIC,
            "claude-3-5-sonnet-20241022" to LlmProvider.ANTHROPIC,
            "claude-3-haiku-20240307" to LlmProvider.ANTHROPIC,
            "claude-3-opus-20240229" to LlmProvider.ANTHROPIC,
            "gpt-4o" to LlmProvider.OPENAI,
            "gpt-4o-mini" to LlmProvider.OPENAI,
            "gpt-4-turbo" to LlmProvider.OPENAI,
            "gpt-3.5-turbo" to LlmProvider.OPENAI,
            "o1" to LlmProvider.OPENAI,
            "o1-mini" to LlmProvider.OPENAI,
            "o3-mini" to LlmProvider.OPENAI,
        )

        private val PROVIDER_API_KEY_ENV_VARS = mapOf(
            LlmProvider.ANTHROPIC to "ANTHROPIC_API_KEY",
            LlmProvider.OPENAI to "OPENAI_API_KEY",
            LlmProvider.GOOGLE to "GOOGLE_API_KEY",
        )
    }

    suspend fun resolveModel(modelAlias: String): ModelConfig {
        // First, try to find in DB
        val dbConfig = modelConfigRepository.findByModelAlias(modelAlias)
        if (dbConfig != null && dbConfig.enabled) {
            logger.debug { "Model config found in DB: $modelAlias -> ${dbConfig.provider}/${dbConfig.modelName}" }
            return dbConfig
        }

        // Fallback to default mappings
        val provider = DEFAULT_MODEL_MAPPINGS[modelAlias]
            ?: inferProviderFromModel(modelAlias)
            ?: throw ApiException(
                status = HttpStatus.BAD_REQUEST,
                message = "Unknown model: $modelAlias",
                errorCode = "model_not_found",
            )

        logger.debug { "Using default model config for: $modelAlias -> $provider" }

        return ModelConfig(
            modelAlias = modelAlias,
            provider = provider,
            modelName = modelAlias,
            endpoint = llmProperties.providers[providerKey(provider)]?.baseUrl ?: "",
            apiKeyEnvVar = PROVIDER_API_KEY_ENV_VARS[provider] ?: "",
            costPerInputToken = 0.0,
            costPerOutputToken = 0.0,
            maxContextTokens = 128_000,
            maxOutputTokens = 4096,
        )
    }

    fun getProviderApiKey(config: ModelConfig): String {
        val envVar = config.apiKeyEnvVar.ifEmpty {
            PROVIDER_API_KEY_ENV_VARS[config.provider]
                ?: throw ApiException(
                    status = HttpStatus.INTERNAL_SERVER_ERROR,
                    message = "No API key environment variable configured for provider: ${config.provider}",
                    errorCode = "missing_provider_config",
                )
        }

        return System.getenv(envVar)
            ?: throw ApiException(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                message = "Provider API key not configured. Set environment variable: $envVar",
                errorCode = "missing_api_key",
            )
    }

    private fun inferProviderFromModel(model: String): LlmProvider? {
        return when {
            model.startsWith("claude") -> LlmProvider.ANTHROPIC
            model.startsWith("gpt") || model.startsWith("o1") || model.startsWith("o3") || model.startsWith("o4") -> LlmProvider.OPENAI
            model.startsWith("gemini") -> LlmProvider.GOOGLE
            else -> null
        }
    }

    private fun providerKey(provider: LlmProvider): String {
        return when (provider) {
            LlmProvider.ANTHROPIC -> "claude"
            LlmProvider.OPENAI -> "openai"
            LlmProvider.GOOGLE -> "gemini"
        }
    }
}
