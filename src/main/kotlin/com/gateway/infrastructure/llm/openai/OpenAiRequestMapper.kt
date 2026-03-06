package com.gateway.infrastructure.llm.openai

import com.gateway.api.dto.request.ChatCompletionRequest

object OpenAiRequestMapper {

    fun toOpenAiRequest(request: ChatCompletionRequest): OpenAiChatCompletionRequest {
        return OpenAiChatCompletionRequest(
            model = request.model,
            messages = request.messages.map { msg ->
                OpenAiMessage(
                    role = msg.role,
                    content = msg.content,
                    name = msg.name,
                )
            },
            temperature = request.temperature,
            maxTokens = request.maxTokens,
            topP = request.topP,
            stream = request.stream,
            stop = request.stop,
            presencePenalty = request.presencePenalty,
            frequencyPenalty = request.frequencyPenalty,
            user = request.user,
        )
    }
}

data class OpenAiChatCompletionRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val stream: Boolean = false,
    val stop: List<String>? = null,
    val presencePenalty: Double? = null,
    val frequencyPenalty: Double? = null,
    val user: String? = null,
)

data class OpenAiMessage(
    val role: String,
    val content: String,
    val name: String? = null,
)
