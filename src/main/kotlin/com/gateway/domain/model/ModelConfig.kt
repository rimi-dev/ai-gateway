package com.gateway.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.Indexed
import java.time.Instant

@Document(collection = "model_configs")
data class ModelConfig(
    @Id val id: String? = null,
    @Indexed(unique = true) val modelAlias: String,
    val provider: LlmProvider,
    val modelName: String,
    val endpoint: String,
    val apiKeyEnvVar: String,
    val costPerInputToken: Double,
    val costPerOutputToken: Double,
    val maxContextTokens: Int,
    val maxOutputTokens: Int = 4096,
    val supportsStreaming: Boolean = true,
    val rateLimitRpm: Int = 1000,
    val rateLimitTpm: Int = 400_000,
    val fallbackChain: List<String> = emptyList(),
    val circuitBreakerConfig: CircuitBreakerSettings = CircuitBreakerSettings(),
    val enabled: Boolean = true,
    @CreatedDate val createdAt: Instant? = null,
    @LastModifiedDate val updatedAt: Instant? = null,
)

enum class LlmProvider {
    ANTHROPIC, OPENAI, GOOGLE
}

data class CircuitBreakerSettings(
    val failureRateThreshold: Float = 50f,
    val slowCallRateThreshold: Float = 80f,
    val slowCallDurationSeconds: Long = 30,
    val slidingWindowSize: Int = 20,
    val waitDurationInOpenStateSeconds: Long = 30,
    val permittedCallsInHalfOpenState: Int = 5,
)
