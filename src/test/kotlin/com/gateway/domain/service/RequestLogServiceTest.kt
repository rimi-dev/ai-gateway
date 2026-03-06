package com.gateway.domain.service

import com.gateway.domain.model.RequestLog
import com.gateway.domain.model.RequestStatus
import com.gateway.domain.repository.RequestLogRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RequestLogServiceTest {

    private lateinit var requestLogRepository: RequestLogRepository
    private lateinit var requestLogService: RequestLogService

    @BeforeEach
    fun setUp() {
        requestLogRepository = mockk()
        requestLogService = RequestLogService(requestLogRepository)
    }

    @Nested
    @DisplayName("saveLog")
    inner class SaveLog {

        @Test
        @DisplayName("로그가 비동기로 저장된다")
        fun `saves log asynchronously via repository`() = runTest {
            // given
            val requestLog = RequestLog(
                requestId = "req-001",
                apiKeyId = "key-001",
                teamId = "team-001",
                projectId = "project-001",
                provider = "openai",
                model = "gpt-4o",
                requestedModel = "gpt-4o",
                status = RequestStatus.SUCCESS,
                inputTokens = 100,
                outputTokens = 50,
                totalTokens = 150,
                costUsd = 0.0025,
                latencyMs = 1200,
            )
            coEvery { requestLogRepository.save(requestLog) } returns requestLog

            // when
            requestLogService.saveLog(requestLog)

            // then - small delay to allow the async coroutine to execute
            delay(200)
            coVerify(exactly = 1) { requestLogRepository.save(requestLog) }
        }

        @Test
        @DisplayName("예외가 발생해도 전파되지 않는다")
        fun `does not propagate exceptions from repository`() = runTest {
            // given
            val requestLog = RequestLog(
                requestId = "req-002",
                apiKeyId = "key-002",
                teamId = "team-002",
                provider = "anthropic",
                model = "claude-sonnet-4-20250514",
                requestedModel = "claude-sonnet-4-20250514",
                status = RequestStatus.FAILED,
                errorCode = "provider_error",
                errorMessage = "Rate limit exceeded",
            )
            coEvery { requestLogRepository.save(requestLog) } throws RuntimeException("DB connection failed")

            // when - should not throw
            requestLogService.saveLog(requestLog)

            // then - small delay to allow the async coroutine to execute and handle the exception
            delay(200)
            coVerify(exactly = 1) { requestLogRepository.save(requestLog) }
        }

        @Test
        @DisplayName("FAILED 상태 로그도 정상적으로 저장된다")
        fun `saves failed status logs correctly`() = runTest {
            // given
            val requestLog = RequestLog(
                requestId = "req-003",
                apiKeyId = "key-003",
                teamId = "team-003",
                provider = "openai",
                model = "gpt-4o-mini",
                requestedModel = "gpt-4o-mini",
                status = RequestStatus.FAILED,
                errorCode = "internal_error",
                errorMessage = "Unexpected error occurred",
                latencyMs = 0,
            )
            coEvery { requestLogRepository.save(requestLog) } returns requestLog

            // when
            requestLogService.saveLog(requestLog)

            // then
            delay(200)
            coVerify(exactly = 1) { requestLogRepository.save(requestLog) }
        }
    }
}
