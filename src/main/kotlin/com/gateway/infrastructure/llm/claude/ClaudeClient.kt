package com.gateway.infrastructure.llm.claude

import tools.jackson.databind.ObjectMapper
import com.gateway.api.dto.request.ChatCompletionRequest
import com.gateway.api.dto.response.ChatCompletionChunkResponse
import com.gateway.api.dto.response.ChatCompletionResponse
import com.gateway.api.exception.ApiException
import com.gateway.config.properties.LlmProperties
import com.gateway.infrastructure.llm.LlmClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono

private val logger = KotlinLogging.logger {}

@Component
class ClaudeClient(
    webClientBuilder: WebClient.Builder,
    private val llmProperties: LlmProperties,
    private val objectMapper: ObjectMapper,
) : LlmClient {

    override val provider: String = "anthropic"

    private val providerConfig = llmProperties.providers["claude"]
    private val webClient: WebClient = webClientBuilder
        .baseUrl(providerConfig?.baseUrl ?: "https://api.anthropic.com")
        .build()

    override suspend fun chatCompletion(request: ChatCompletionRequest, apiKey: String): ChatCompletionResponse {
        val claudeRequest = ClaudeRequestMapper.toClaudeRequest(request.copy(stream = false))

        logger.debug { "Sending Claude request for model: ${request.model}" }

        val response = webClient.post()
            .uri("/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", providerConfig?.apiVersion ?: "2024-01-01")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(claudeRequest)
            .retrieve()
            .onStatus({ it.isError }) { clientResponse ->
                clientResponse.bodyToMono<String>().map { body ->
                    logger.error { "Claude API error: ${clientResponse.statusCode()} - $body" }
                    ApiException(
                        status = HttpStatus.valueOf(clientResponse.statusCode().value()),
                        message = "Claude API error: $body",
                        errorCode = "llm_provider_error",
                    )
                }
            }
            .bodyToMono<ClaudeMessagesResponse>()
            .awaitSingle()

        return ClaudeResponseMapper.toChatCompletionResponse(response)
    }

    override fun chatCompletionStream(request: ChatCompletionRequest, apiKey: String): Flow<ChatCompletionChunkResponse> {
        val claudeRequest = ClaudeRequestMapper.toClaudeRequest(request.copy(stream = true))

        return flow {
            val eventFlux = webClient.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", providerConfig?.apiVersion ?: "2024-01-01")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(claudeRequest)
                .retrieve()
                .onStatus({ it.isError }) { clientResponse ->
                    clientResponse.bodyToMono<String>().map { body ->
                        logger.error { "Claude streaming API error: ${clientResponse.statusCode()} - $body" }
                        ApiException(
                            status = HttpStatus.valueOf(clientResponse.statusCode().value()),
                            message = "Claude API error: $body",
                            errorCode = "llm_provider_error",
                        )
                    }
                }
                .bodyToFlux<String>()

            eventFlux.asFlow().collect { line ->
                if (line.startsWith("data: ")) {
                    val json = line.removePrefix("data: ").trim()
                    if (json.isNotEmpty()) {
                        try {
                            val event = objectMapper.readValue(json, ClaudeStreamEvent::class.java)
                            val chunk = ClaudeResponseMapper.toChunkResponse(event, request.model)
                            if (chunk != null) {
                                emit(chunk)
                            }
                        } catch (e: Exception) {
                            logger.warn { "Failed to parse Claude stream event: $json" }
                        }
                    }
                }
            }
        }
    }

    override fun supports(model: String): Boolean {
        return model.startsWith("claude")
    }
}
