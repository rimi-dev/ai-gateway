package com.gateway.domain.service

import com.gateway.api.exception.ApiException
import com.gateway.domain.model.ApiKey
import com.gateway.domain.repository.ApiKeyRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository,
) {

    suspend fun validateAndGet(key: String): ApiKey {
        val apiKey = apiKeyRepository.findByKey(key)
            ?: throw ApiException(
                status = HttpStatus.UNAUTHORIZED,
                message = "Invalid API key",
                errorCode = "invalid_api_key",
            )

        if (!apiKey.enabled) {
            throw ApiException(
                status = HttpStatus.FORBIDDEN,
                message = "API key is disabled",
                errorCode = "api_key_disabled",
            )
        }

        logger.debug { "API key validated: name=${apiKey.name}, team=${apiKey.teamId}" }
        return apiKey
    }
}
