package com.gateway.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "routing_rules")
data class RoutingRule(
    @Id val id: String? = null,
    val name: String,
    val description: String? = null,
    val priority: Int = 0,
    val condition: RoutingCondition,
    val targetModel: String,
    val enabled: Boolean = true,
    @CreatedDate val createdAt: Instant? = null,
    @LastModifiedDate val updatedAt: Instant? = null,
)

data class RoutingCondition(
    val maxInputTokens: Int? = null,
    val minInputTokens: Int? = null,
    val budgetMode: Boolean? = null,
    val qualityMode: Boolean? = null,
    val teamId: String? = null,
    val headerMatch: Map<String, String>? = null,
)
