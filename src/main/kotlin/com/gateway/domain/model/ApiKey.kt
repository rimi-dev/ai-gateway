package com.gateway.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.Indexed
import java.time.Instant

@Document(collection = "api_keys")
data class ApiKey(
    @Id val id: String? = null,
    @Indexed(unique = true) val key: String,
    val name: String,
    val teamId: String,
    val projectId: String? = null,
    val permissions: Set<Permission> = setOf(Permission.CHAT),
    val allowedModels: Set<String> = emptySet(),
    val rateLimitRpm: Int = 60,
    val rateLimitTpm: Int = 100_000,
    val budget: Budget? = null,
    val enabled: Boolean = true,
    @CreatedDate val createdAt: Instant? = null,
    @LastModifiedDate val updatedAt: Instant? = null,
)

enum class Permission { CHAT, EMBEDDING, ADMIN }

data class Budget(
    val monthlyLimitUsd: Double,
    val currentUsageUsd: Double = 0.0,
    val currency: String = "USD",
)
