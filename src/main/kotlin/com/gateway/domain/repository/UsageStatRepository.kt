package com.gateway.domain.repository

import com.gateway.domain.model.UsageStat
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDate

interface UsageStatRepository : CoroutineCrudRepository<UsageStat, String> {
    fun findByTeamIdAndDateBetween(teamId: String, startDate: LocalDate, endDate: LocalDate): Flow<UsageStat>
    fun findByModelAndDateBetween(model: String, startDate: LocalDate, endDate: LocalDate): Flow<UsageStat>
    fun findByDateBetween(startDate: LocalDate, endDate: LocalDate): Flow<UsageStat>
}
