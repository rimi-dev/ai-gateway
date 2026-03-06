package com.gateway.domain.service

import com.gateway.api.dto.request.ChatCompletionRequest
import com.gateway.api.dto.response.ChatCompletionResponse
import com.gateway.api.exception.ApiException
import com.gateway.domain.model.ApiKey
import com.gateway.domain.model.RequestLog
import com.gateway.domain.model.RequestStatus
import com.gateway.infrastructure.llm.LlmClientFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

@Service
class ProxyService(
    private val llmClientFactory: LlmClientFactory,
    private val modelConfigService: ModelConfigService,
    private val requestLogService: RequestLogService,
) {

    suspend fun chatCompletion(request: ChatCompletionRequest, apiKey: ApiKey): ChatCompletionResponse {
        val requestId = UUID.randomUUID().toString()

        // Validate model access
        if (apiKey.allowedModels.isNotEmpty() && request.model !in apiKey.allowedModels) {
            throw ApiException(
                status = HttpStatus.FORBIDDEN,
                message = "Model '${request.model}' is not allowed for this API key",
                errorCode = "model_not_allowed",
            )
        }

        // Resolve model configuration
        val modelConfig = modelConfigService.resolveModel(request.model)
        val providerApiKey = modelConfigService.getProviderApiKey(modelConfig)

        // Get the appropriate LLM client
        val client = llmClientFactory.getClientByProvider(
            modelConfig.provider.name.lowercase(),
        )

        logger.info { "Proxying request: id=$requestId, model=${request.model}, provider=${client.provider}, team=${apiKey.teamId}" }

        var response: ChatCompletionResponse
        val latencyMs: Long

        try {
            latencyMs = measureTimeMillis {
                response = client.chatCompletion(request, providerApiKey)
            }

            // Add gateway metadata
            response = response.copy(
                provider = client.provider,
                latencyMs = latencyMs,
            )

            // Fire-and-forget log
            requestLogService.saveLog(
                RequestLog(
                    requestId = requestId,
                    apiKeyId = apiKey.id ?: "",
                    teamId = apiKey.teamId,
                    projectId = apiKey.projectId,
                    provider = client.provider,
                    model = response.model,
                    requestedModel = request.model,
                    status = RequestStatus.SUCCESS,
                    inputTokens = response.usage.promptTokens,
                    outputTokens = response.usage.completionTokens,
                    totalTokens = response.usage.totalTokens,
                    costUsd = calculateCost(
                        modelConfig.costPerInputToken,
                        modelConfig.costPerOutputToken,
                        response.usage.promptTokens,
                        response.usage.completionTokens,
                    ),
                    latencyMs = latencyMs,
                ),
            )

            logger.info { "Request completed: id=$requestId, latency=${latencyMs}ms, tokens=${response.usage.totalTokens}" }

            return response
        } catch (ex: ApiException) {
            requestLogService.saveLog(
                RequestLog(
                    requestId = requestId,
                    apiKeyId = apiKey.id ?: "",
                    teamId = apiKey.teamId,
                    projectId = apiKey.projectId,
                    provider = client.provider,
                    model = request.model,
                    requestedModel = request.model,
                    status = RequestStatus.FAILED,
                    errorCode = ex.errorCode,
                    errorMessage = ex.message,
                    latencyMs = 0,
                ),
            )
            throw ex
        } catch (ex: Exception) {
            logger.error(ex) { "Unexpected error in proxy: requestId=$requestId" }

            requestLogService.saveLog(
                RequestLog(
                    requestId = requestId,
                    apiKeyId = apiKey.id ?: "",
                    teamId = apiKey.teamId,
                    projectId = apiKey.projectId,
                    provider = client.provider,
                    model = request.model,
                    requestedModel = request.model,
                    status = RequestStatus.FAILED,
                    errorCode = "internal_error",
                    errorMessage = ex.message,
                    latencyMs = 0,
                ),
            )

            throw ApiException(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                message = "Failed to process request: ${ex.message}",
                errorCode = "internal_error",
            )
        }
    }

    private fun calculateCost(
        costPerInputToken: Double,
        costPerOutputToken: Double,
        inputTokens: Int,
        outputTokens: Int,
    ): Double {
        return (costPerInputToken * inputTokens) + (costPerOutputToken * outputTokens)
    }
}
