package com.gateway.api.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChatCompletionChunkResponse(
    val id: String,
    @JsonProperty("object") val objectType: String = "chat.completion.chunk",
    val created: Long,
    val model: String,
    val choices: List<ChunkChoice>,
    val usage: TokenUsage? = null,
    @JsonProperty("system_fingerprint") val systemFingerprint: String? = null,
)

data class ChunkChoice(
    val index: Int,
    val delta: Delta,
    @JsonProperty("finish_reason") val finishReason: String?,
    val logprobs: Any? = null,
)

data class Delta(
    val role: String? = null,
    val content: String? = null,
)
