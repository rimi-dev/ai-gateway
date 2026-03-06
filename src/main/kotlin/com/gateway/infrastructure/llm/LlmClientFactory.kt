package com.gateway.infrastructure.llm

import com.gateway.api.exception.ApiException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class LlmClientFactory(
    private val clients: List<LlmClient>,
) {
    fun getClient(model: String): LlmClient =
        clients.firstOrNull { it.supports(model) }
            ?: throw ApiException(
                status = HttpStatus.BAD_REQUEST,
                message = "No LLM client found for model: $model",
                errorCode = "model_not_supported",
            )

    fun getClientByProvider(provider: String): LlmClient =
        clients.firstOrNull { it.provider.equals(provider, ignoreCase = true) }
            ?: throw ApiException(
                status = HttpStatus.BAD_REQUEST,
                message = "No LLM client found for provider: $provider",
                errorCode = "provider_not_supported",
            )
}
