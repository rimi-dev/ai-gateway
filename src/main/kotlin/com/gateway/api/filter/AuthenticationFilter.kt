package com.gateway.api.filter

import tools.jackson.databind.ObjectMapper
import com.gateway.api.dto.response.ErrorDetail
import com.gateway.api.dto.response.ErrorResponse
import com.gateway.api.exception.ApiException
import com.gateway.domain.service.ApiKeyService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange

private val logger = KotlinLogging.logger {}

@Component
@Order(-100)
class AuthenticationFilter(
    private val apiKeyService: ApiKeyService,
    private val objectMapper: ObjectMapper,
) : CoWebFilter() {

    companion object {
        private val SKIP_PATHS = listOf(
            "/actuator",
            "/api/v1/admin/health",
        )
    }

    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        val path = exchange.request.uri.path

        if (shouldSkip(path)) {
            return chain.filter(exchange)
        }

        val authHeader = exchange.request.headers.getFirst("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeErrorResponse(
                exchange,
                HttpStatus.UNAUTHORIZED,
                "Missing or invalid Authorization header. Expected: Bearer <api_key>",
                "missing_api_key",
            )
            return
        }

        val key = authHeader.removePrefix("Bearer ").trim()

        try {
            val apiKey = apiKeyService.validateAndGet(key)
            exchange.attributes["apiKey"] = apiKey
            chain.filter(exchange)
        } catch (ex: ApiException) {
            writeErrorResponse(exchange, ex.status, ex.message, ex.errorCode)
        }
    }

    private fun shouldSkip(path: String): Boolean {
        return SKIP_PATHS.any { path.startsWith(it) }
    }

    private fun writeErrorResponse(
        exchange: ServerWebExchange,
        status: HttpStatus,
        message: String,
        errorCode: String?,
    ) {
        val response = exchange.response
        response.statusCode = status
        response.headers.contentType = MediaType.APPLICATION_JSON

        val errorResponse = ErrorResponse(
            error = ErrorDetail(
                message = message,
                type = "authentication_error",
                code = errorCode,
            ),
        )

        val bytes = objectMapper.writeValueAsBytes(errorResponse)
        val buffer = response.bufferFactory().wrap(bytes)
        response.writeWith(reactor.core.publisher.Mono.just(buffer)).subscribe()
    }
}
