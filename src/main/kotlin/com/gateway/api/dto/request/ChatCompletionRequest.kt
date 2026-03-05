package com.gateway.api.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double? = null,
    @JsonProperty("max_tokens") val maxTokens: Int? = null,
    @JsonProperty("top_p") val topP: Double? = null,
    val stream: Boolean = false,
    val stop: List<String>? = null,
    @JsonProperty("presence_penalty") val presencePenalty: Double? = null,
    @JsonProperty("frequency_penalty") val frequencyPenalty: Double? = null,
    val user: String? = null,
    // Gateway custom headers (optional)
    @JsonProperty("x-priority") val priority: String? = null,
    @JsonProperty("x-cache") val cacheEnabled: Boolean? = null,
)

data class Message(
    val role: String,
    val content: String,
    val name: String? = null,
)
