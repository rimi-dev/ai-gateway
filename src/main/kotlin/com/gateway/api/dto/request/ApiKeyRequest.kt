package com.gateway.api.dto.request

data class CreateApiKeyRequest(
    val name: String,
    val teamId: String,
    val projectId: String? = null,
    val permissions: Set<String> = setOf("CHAT"),
    val allowedModels: Set<String> = emptySet(),
    val rateLimitRpm: Int = 60,
    val rateLimitTpm: Int = 100_000,
    val monthlyBudgetUsd: Double? = null,
)

data class UpdateApiKeyRequest(
    val name: String? = null,
    val permissions: Set<String>? = null,
    val allowedModels: Set<String>? = null,
    val rateLimitRpm: Int? = null,
    val rateLimitTpm: Int? = null,
    val monthlyBudgetUsd: Double? = null,
    val enabled: Boolean? = null,
)
