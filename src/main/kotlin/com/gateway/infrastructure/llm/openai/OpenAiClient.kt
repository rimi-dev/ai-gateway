package com.gateway.infrastructure.llm.openai

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
class OpenAiClient(
    webClientBuilder: WebClient.Builder,
    private val llmProperties: LlmProperties,
    private val objectMapper: ObjectMapper,
) : LlmClient {

    override val provider: String = "openai"

    private val providerConfig = llmProperties.providers["openai"]
    private val webClient: WebClient = webClientBuilder
        .baseUrl(providerConfig?.baseUrl ?: "https://api.openai.com")
        .build()

    override suspend fun chatCompletion(request: ChatCompletionRequest, apiKey: String): ChatCompletionResponse {
        val openAiRequest = OpenAiRequestMapper.toOpenAiRequest(request.copy(stream = false))

        logger.debug { "Sending OpenAI request for model: ${request.model}" }

        val response = webClient.post()
            .uri("/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(openAiRequest)
            .retrieve()
            .onStatus({ it.isError }) { clientResponse ->
                clientResponse.bodyToMono<String>().map { body ->
                    logger.error { "OpenAI API error: ${clientResponse.statusCode()} - $body" }
                    ApiException(
                        status = HttpStatus.valueOf(clientResponse.statusCode().value()),
                        message = "OpenAI API error: $body",
                        errorCode = "llm_provider_error",
                    )
                }
            }
            .bodyToMono<OpenAiChatCompletionResponse>()
            .awaitSingle()

        return OpenAiResponseMapper.toChatCompletionResponse(response)
    }

    override fun chatCompletionStream(request: ChatCompletionRequest, apiKey: String): Flow<ChatCompletionChunkResponse> {
        val openAiRequest = OpenAiRequestMapper.toOpenAiRequest(request.copy(stream = true))

        return flow {
            val lineFlux = webClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(openAiRequest)
                .retrieve()
                .onStatus({ it.isError }) { clientResponse ->
                    clientResponse.bodyToMono<String>().map { body ->
                        logger.error { "OpenAI streaming API error: ${clientResponse.statusCode()} - $body" }
                        ApiException(
                            status = HttpStatus.valueOf(clientResponse.statusCode().value()),
                            message = "OpenAI API error: $body",
                            errorCode = "llm_provider_error",
                        )
                    }
                }
                .bodyToFlux<String>()

            lineFlux.asFlow().collect { line ->
                if (line.startsWith("data: ")) {
                    val json = line.removePrefix("data: ").trim()
                    if (json.isNotEmpty() && json != "[DONE]") {
                        try {
                            val chunk = objectMapper.readValue(json, OpenAiStreamChunkResponse::class.java)
                            emit(OpenAiResponseMapper.toChunkResponse(chunk))
                        } catch (e: Exception) {
                            logger.warn { "Failed to parse OpenAI stream chunk: $json" }
                        }
                    }
                }
            }
        }
    }

    override fun supports(model: String): Boolean {
        return model.startsWith("gpt") || model.startsWith("o1") || model.startsWith("o3") || model.startsWith("o4")
    }
}
