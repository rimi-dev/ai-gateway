package com.gateway.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "gateway.llm")
data class LlmProperties(
    val providers: Map<String, ProviderConfig> = emptyMap(),
)

data class ProviderConfig(
    val baseUrl: String,
    val apiVersion: String? = null,
    val defaultModel: String,
    val timeout: Duration = Duration.ofSeconds(60),
    val maxRetries: Int = 3,
)
