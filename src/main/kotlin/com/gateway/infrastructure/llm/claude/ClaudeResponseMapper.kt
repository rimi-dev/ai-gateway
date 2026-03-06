package com.gateway.infrastructure.llm.claude

import com.gateway.api.dto.request.Message
import com.gateway.api.dto.response.ChatCompletionChunkResponse
import com.gateway.api.dto.response.ChatCompletionResponse
import com.gateway.api.dto.response.Choice
import com.gateway.api.dto.response.ChunkChoice
import com.gateway.api.dto.response.Delta
import com.gateway.api.dto.response.TokenUsage
import java.time.Instant

object ClaudeResponseMapper {

    fun toChatCompletionResponse(response: ClaudeMessagesResponse): ChatCompletionResponse {
        val text = response.content
            .firstOrNull { it.type == "text" }
            ?.text ?: ""

        return ChatCompletionResponse(
            id = response.id,
            objectType = "chat.completion",
            created = Instant.now().epochSecond,
            model = response.model,
            choices = listOf(
                Choice(
                    index = 0,
                    message = Message(role = "assistant", content = text),
                    finishReason = mapStopReason(response.stopReason),
                ),
            ),
            usage = TokenUsage(
                promptTokens = response.usage.inputTokens,
                completionTokens = response.usage.outputTokens,
                totalTokens = response.usage.inputTokens + response.usage.outputTokens,
            ),
        )
    }

    fun toChunkResponse(event: ClaudeStreamEvent, model: String): ChatCompletionChunkResponse? {
        return when (event.type) {
            "content_block_delta" -> {
                val text = event.delta?.text ?: return null
                ChatCompletionChunkResponse(
                    id = "chatcmpl-stream",
                    objectType = "chat.completion.chunk",
                    created = Instant.now().epochSecond,
                    model = model,
                    choices = listOf(
                        ChunkChoice(
                            index = 0,
                            delta = Delta(content = text),
                            finishReason = null,
                        ),
                    ),
                )
            }
            "message_start" -> {
                ChatCompletionChunkResponse(
                    id = event.message?.id ?: "chatcmpl-stream",
                    objectType = "chat.completion.chunk",
                    created = Instant.now().epochSecond,
                    model = event.message?.model ?: model,
                    choices = listOf(
                        ChunkChoice(
                            index = 0,
                            delta = Delta(role = "assistant"),
                            finishReason = null,
                        ),
                    ),
                )
            }
            "message_delta" -> {
                ChatCompletionChunkResponse(
                    id = "chatcmpl-stream",
                    objectType = "chat.completion.chunk",
                    created = Instant.now().epochSecond,
                    model = model,
                    choices = listOf(
                        ChunkChoice(
                            index = 0,
                            delta = Delta(),
                            finishReason = event.delta?.stopReason?.let { mapStopReason(it) },
                        ),
                    ),
                    usage = event.usage?.let {
                        TokenUsage(
                            promptTokens = it.inputTokens,
                            completionTokens = it.outputTokens,
                            totalTokens = it.inputTokens + it.outputTokens,
                        )
                    },
                )
            }
            else -> null
        }
    }

    private fun mapStopReason(stopReason: String?): String {
        return when (stopReason) {
            "end_turn" -> "stop"
            "max_tokens" -> "length"
            "stop_sequence" -> "stop"
            else -> "stop"
        }
    }
}

data class ClaudeMessagesResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContent>,
    val model: String,
    val stopReason: String?,
    val usage: ClaudeUsage,
)

data class ClaudeContent(
    val type: String,
    val text: String? = null,
)

data class ClaudeUsage(
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
)

data class ClaudeStreamEvent(
    val type: String,
    val message: ClaudeStreamMessage? = null,
    val delta: ClaudeStreamDelta? = null,
    val usage: ClaudeUsage? = null,
    val index: Int? = null,
    val contentBlock: ClaudeContent? = null,
)

data class ClaudeStreamMessage(
    val id: String,
    val type: String? = null,
    val role: String? = null,
    val model: String? = null,
    val usage: ClaudeUsage? = null,
)

data class ClaudeStreamDelta(
    val type: String? = null,
    val text: String? = null,
    val stopReason: String? = null,
)
