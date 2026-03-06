package com.gateway.api.proxy

import com.gateway.api.dto.request.ChatCompletionRequest
import com.gateway.api.dto.response.ChatCompletionResponse
import com.gateway.api.exception.ApiException
import com.gateway.domain.model.ApiKey
import com.gateway.domain.service.ProxyService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

@RestController
@RequestMapping("/api/v1")
class ProxyController(
    private val proxyService: ProxyService,
) {

    @PostMapping("/chat/completions")
    suspend fun chatCompletions(
        @RequestBody request: ChatCompletionRequest,
        exchange: ServerWebExchange,
    ): ChatCompletionResponse {
        val apiKey = exchange.getAttribute<ApiKey>("apiKey")
            ?: throw ApiException(
                status = HttpStatus.UNAUTHORIZED,
                message = "Authentication required",
                errorCode = "unauthenticated",
            )

        return proxyService.chatCompletion(request, apiKey)
    }
}
