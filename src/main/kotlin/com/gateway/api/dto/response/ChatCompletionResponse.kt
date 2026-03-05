package com.gateway.api.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.gateway.api.dto.request.Message

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChatCompletionResponse(
    val id: String,
    @JsonProperty("object") val objectType: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: TokenUsage,
    @JsonProperty("system_fingerprint") val systemFingerprint: String? = null,
    // Gateway extensions
    @JsonProperty("x-provider") val provider: String? = null,
    @JsonProperty("x-cached") val cached: Boolean? = null,
    @JsonProperty("x-latency-ms") val latencyMs: Long? = null,
    @JsonProperty("x-fallback-from") val fallbackFrom: String? = null,
)

data class Choice(
    val index: Int,
    val message: Message,
    @JsonProperty("finish_reason") val finishReason: String?,
    val logprobs: Any? = null,
)

data class TokenUsage(
    @JsonProperty("prompt_tokens") val promptTokens: Int,
    @JsonProperty("completion_tokens") val completionTokens: Int,
    @JsonProperty("total_tokens") val totalTokens: Int,
)
