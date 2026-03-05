package com.gateway.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.CompoundIndex
import java.time.LocalDate

@Document(collection = "usage_stats")
@CompoundIndex(name = "team_date_idx", def = "{'teamId': 1, 'date': -1}")
@CompoundIndex(name = "model_date_idx", def = "{'model': 1, 'date': -1}")
data class UsageStat(
    @Id val id: String? = null,
    val teamId: String,
    val projectId: String? = null,
    val model: String,
    val provider: String,
    val date: LocalDate,
    val requestCount: Long = 0,
    val successCount: Long = 0,
    val failureCount: Long = 0,
    val cacheHitCount: Long = 0,
    val fallbackCount: Long = 0,
    val totalInputTokens: Long = 0,
    val totalOutputTokens: Long = 0,
    val totalCostUsd: Double = 0.0,
    val avgLatencyMs: Double = 0.0,
    val p95LatencyMs: Double = 0.0,
    val p99LatencyMs: Double = 0.0,
)
