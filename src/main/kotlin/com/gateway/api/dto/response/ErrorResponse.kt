package com.gateway.api.dto.response

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val error: ErrorDetail,
)

data class ErrorDetail(
    val message: String,
    val type: String,
    val param: String? = null,
    val code: String? = null,
)
