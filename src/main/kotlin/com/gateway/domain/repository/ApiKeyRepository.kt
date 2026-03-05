package com.gateway.domain.repository

import com.gateway.domain.model.ApiKey
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ApiKeyRepository : CoroutineCrudRepository<ApiKey, String> {
    suspend fun findByKey(key: String): ApiKey?
    suspend fun findByTeamId(teamId: String): List<ApiKey>
    suspend fun findByEnabled(enabled: Boolean): List<ApiKey>
}
