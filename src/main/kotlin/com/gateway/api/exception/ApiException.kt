package com.gateway.api.exception

import org.springframework.http.HttpStatus

class ApiException(
    val status: HttpStatus,
    override val message: String,
    val errorCode: String? = null,
) : RuntimeException(message)
