package com.gateway.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.Indexed
import java.time.Instant

@Document(collection = "request_logs")
data class RequestLog(
    @Id val id: String? = null,
    val requestId: String,
    @Indexed val apiKeyId: String,
    @Indexed val teamId: String,
    val projectId: String? = null,
    val provider: String,
    @Indexed val model: String,
    val requestedModel: String,
    @Indexed val status: RequestStatus,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val totalTokens: Int = 0,
    val costUsd: Double = 0.0,
    val latencyMs: Long = 0,
    val ttftMs: Long? = null,
    val cacheHit: Boolean = false,
    val fallbackUsed: Boolean = false,
    val fallbackFrom: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    @CreatedDate val createdAt: Instant? = null,
)

enum class RequestStatus {
    SUCCESS, FAILED, FALLBACK, CACHED, RATE_LIMITED, TIMEOUT
}
