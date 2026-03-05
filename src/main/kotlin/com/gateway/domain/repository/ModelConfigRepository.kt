package com.gateway.domain.repository

import com.gateway.domain.model.LlmProvider
import com.gateway.domain.model.ModelConfig
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ModelConfigRepository : CoroutineCrudRepository<ModelConfig, String> {
    suspend fun findByModelAlias(modelAlias: String): ModelConfig?
    suspend fun findByProvider(provider: LlmProvider): List<ModelConfig>
    suspend fun findByEnabled(enabled: Boolean): List<ModelConfig>
}
