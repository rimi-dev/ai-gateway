package com.gateway.infrastructure.llm.claude

import com.gateway.api.dto.request.ChatCompletionRequest

object ClaudeRequestMapper {

    fun toClaudeRequest(request: ChatCompletionRequest): ClaudeMessagesRequest {
        val systemMessage = request.messages
            .filter { it.role == "system" }
            .joinToString("\n") { it.content }
            .ifEmpty { null }

        val messages = request.messages
            .filter { it.role != "system" }
            .map { ClaudeMessage(role = it.role, content = it.content) }

        return ClaudeMessagesRequest(
            model = request.model,
            maxTokens = request.maxTokens ?: 4096,
            system = systemMessage,
            messages = messages,
            temperature = request.temperature,
            topP = request.topP,
            stopSequences = request.stop,
            stream = request.stream,
        )
    }
}

data class ClaudeMessagesRequest(
    val model: String,
    val maxTokens: Int,
    val system: String? = null,
    val messages: List<ClaudeMessage>,
    val temperature: Double? = null,
    val topP: Double? = null,
    val stopSequences: List<String>? = null,
    val stream: Boolean = false,
)

data class ClaudeMessage(
    val role: String,
    val content: String,
)
