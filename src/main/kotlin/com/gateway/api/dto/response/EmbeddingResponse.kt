package com.gateway.api.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class EmbeddingResponse(
    @JsonProperty("object") val objectType: String = "list",
    val data: List<EmbeddingData>,
    val model: String,
    val usage: EmbeddingUsage,
)

data class EmbeddingData(
    @JsonProperty("object") val objectType: String = "embedding",
    val embedding: List<Double>,
    val index: Int,
)

data class EmbeddingUsage(
    @JsonProperty("prompt_tokens") val promptTokens: Int,
    @JsonProperty("total_tokens") val totalTokens: Int,
)
