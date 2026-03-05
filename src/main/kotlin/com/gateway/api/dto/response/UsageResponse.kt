package com.gateway.api.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UsageStatsResponse(
    val period: String,
    val stats: List<UsageStatEntry>,
    val totals: UsageTotals,
)

data class UsageStatEntry(
    val date: String,
    val model: String? = null,
    val teamId: String? = null,
    @JsonProperty("request_count") val requestCount: Long,
    @JsonProperty("success_count") val successCount: Long,
    @JsonProperty("input_tokens") val inputTokens: Long,
    @JsonProperty("output_tokens") val outputTokens: Long,
    @JsonProperty("total_cost_usd") val totalCostUsd: Double,
    @JsonProperty("avg_latency_ms") val avgLatencyMs: Double,
    @JsonProperty("cache_hit_rate") val cacheHitRate: Double? = null,
)

data class UsageTotals(
    @JsonProperty("total_requests") val totalRequests: Long,
    @JsonProperty("total_tokens") val totalTokens: Long,
    @JsonProperty("total_cost_usd") val totalCostUsd: Double,
    @JsonProperty("avg_latency_ms") val avgLatencyMs: Double,
    @JsonProperty("cache_hit_rate") val cacheHitRate: Double,
)
