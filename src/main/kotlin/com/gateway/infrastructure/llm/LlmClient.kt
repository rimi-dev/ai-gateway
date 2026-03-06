package com.gateway.infrastructure.llm

import com.gateway.api.dto.request.ChatCompletionRequest
import com.gateway.api.dto.response.ChatCompletionChunkResponse
import com.gateway.api.dto.response.ChatCompletionResponse
import kotlinx.coroutines.flow.Flow

interface LlmClient {
    val provider: String

    suspend fun chatCompletion(request: ChatCompletionRequest, apiKey: String): ChatCompletionResponse

    fun chatCompletionStream(request: ChatCompletionRequest, apiKey: String): Flow<ChatCompletionChunkResponse>

    fun supports(model: String): Boolean
}
