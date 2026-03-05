package com.gateway.domain.repository

import com.gateway.domain.model.RoutingRule
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface RoutingRuleRepository : CoroutineCrudRepository<RoutingRule, String> {
    suspend fun findByEnabledOrderByPriorityDesc(enabled: Boolean): List<RoutingRule>
}
