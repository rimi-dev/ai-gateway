package com.gateway.infrastructure.llm.openai

import com.gateway.api.dto.request.Message
import com.gateway.api.dto.response.ChatCompletionChunkResponse
import com.gateway.api.dto.response.ChatCompletionResponse
import com.gateway.api.dto.response.Choice
import com.gateway.api.dto.response.ChunkChoice
import com.gateway.api.dto.response.Delta
import com.gateway.api.dto.response.TokenUsage

object OpenAiResponseMapper {

    fun toChatCompletionResponse(response: OpenAiChatCompletionResponse): ChatCompletionResponse {
        return ChatCompletionResponse(
            id = response.id,
            objectType = response.objectType,
            created = response.created,
            model = response.model,
            choices = response.choices.map { choice ->
                Choice(
                    index = choice.index,
                    message = Message(
                        role = choice.message.role,
                        content = choice.message.content ?: "",
                    ),
                    finishReason = choice.finishReason,
                )
            },
            usage = TokenUsage(
                promptTokens = response.usage.promptTokens,
                completionTokens = response.usage.completionTokens,
                totalTokens = response.usage.totalTokens,
            ),
            systemFingerprint = response.systemFingerprint,
        )
    }

    fun toChunkResponse(response: OpenAiStreamChunkResponse): ChatCompletionChunkResponse {
        return ChatCompletionChunkResponse(
            id = response.id,
            objectType = response.objectType,
            created = response.created,
            model = response.model,
            choices = response.choices.map { choice ->
                ChunkChoice(
                    index = choice.index,
                    delta = Delta(
                        role = choice.delta.role,
                        content = choice.delta.content,
                    ),
                    finishReason = choice.finishReason,
                )
            },
            usage = response.usage?.let {
                TokenUsage(
                    promptTokens = it.promptTokens,
                    completionTokens = it.completionTokens,
                    totalTokens = it.totalTokens,
                )
            },
            systemFingerprint = response.systemFingerprint,
        )
    }
}

data class OpenAiChatCompletionResponse(
    val id: String,
    val objectType: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage,
    val systemFingerprint: String? = null,
)

data class OpenAiChoice(
    val index: Int,
    val message: OpenAiResponseMessage,
    val finishReason: String?,
)

data class OpenAiResponseMessage(
    val role: String,
    val content: String? = null,
)

data class OpenAiUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
)

data class OpenAiStreamChunkResponse(
    val id: String,
    val objectType: String = "chat.completion.chunk",
    val created: Long,
    val model: String,
    val choices: List<OpenAiStreamChoice>,
    val usage: OpenAiUsage? = null,
    val systemFingerprint: String? = null,
)

data class OpenAiStreamChoice(
    val index: Int,
    val delta: OpenAiStreamDelta,
    val finishReason: String? = null,
)

data class OpenAiStreamDelta(
    val role: String? = null,
    val content: String? = null,
)
