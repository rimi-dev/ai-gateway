package com.gateway.api.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class EmbeddingRequest(
    val input: Any, // String or List<String>
    val model: String,
    @JsonProperty("encoding_format") val encodingFormat: String? = null,
    val user: String? = null,
)
