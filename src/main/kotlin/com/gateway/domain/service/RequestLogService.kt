package com.gateway.domain.service

import com.gateway.domain.model.RequestLog
import com.gateway.domain.repository.RequestLogRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class RequestLogService(
    private val requestLogRepository: RequestLogRepository,
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun saveLog(requestLog: RequestLog) {
        scope.launch {
            try {
                requestLogRepository.save(requestLog)
                logger.debug { "Request log saved: requestId=${requestLog.requestId}, model=${requestLog.model}, status=${requestLog.status}" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to save request log: requestId=${requestLog.requestId}" }
            }
        }
    }
}
