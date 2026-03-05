package com.gateway.domain.repository

import com.gateway.domain.model.RequestLog
import com.gateway.domain.model.RequestStatus
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.Instant

interface RequestLogRepository : CoroutineCrudRepository<RequestLog, String> {
    fun findByTeamIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        teamId: String, start: Instant, end: Instant
    ): Flow<RequestLog>

    fun findByModelAndCreatedAtBetweenOrderByCreatedAtDesc(
        model: String, start: Instant, end: Instant
    ): Flow<RequestLog>

    fun findByApiKeyIdOrderByCreatedAtDesc(
        apiKeyId: String, pageable: Pageable
    ): Flow<RequestLog>

    fun findByStatusOrderByCreatedAtDesc(
        status: RequestStatus, pageable: Pageable
    ): Flow<RequestLog>

    suspend fun countByTeamIdAndCreatedAtBetween(
        teamId: String, start: Instant, end: Instant
    ): Long
}
