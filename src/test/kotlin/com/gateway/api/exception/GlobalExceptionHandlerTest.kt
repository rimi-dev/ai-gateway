package com.gateway.api.exception

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ServerWebInputException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Nested
    @DisplayName("handleApiException")
    inner class HandleApiException {

        @Test
        @DisplayName("UNAUTHORIZED 상태일 때 authentication_error 타입을 반환한다")
        fun `UNAUTHORIZED status returns authentication_error type`() {
            val ex = ApiException(
                status = HttpStatus.UNAUTHORIZED,
                message = "Unauthorized",
                errorCode = "unauthenticated",
            )

            val response = handler.handleApiException(ex)

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            assertEquals("authentication_error", response.body!!.error.type)
            assertEquals("Unauthorized", response.body!!.error.message)
            assertEquals("unauthenticated", response.body!!.error.code)
        }

        @Test
        @DisplayName("FORBIDDEN 상태일 때 permission_error 타입을 반환한다")
        fun `FORBIDDEN status returns permission_error type`() {
            val ex = ApiException(
                status = HttpStatus.FORBIDDEN,
                message = "Forbidden",
                errorCode = "forbidden",
            )

            val response = handler.handleApiException(ex)

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
            assertEquals("permission_error", response.body!!.error.type)
            assertEquals("Forbidden", response.body!!.error.message)
            assertEquals("forbidden", response.body!!.error.code)
        }

        @Test
        @DisplayName("TOO_MANY_REQUESTS 상태일 때 rate_limit_error 타입을 반환한다")
        fun `TOO_MANY_REQUESTS status returns rate_limit_error type`() {
            val ex = ApiException(
                status = HttpStatus.TOO_MANY_REQUESTS,
                message = "Rate limit exceeded",
                errorCode = "rate_limit",
            )

            val response = handler.handleApiException(ex)

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.statusCode)
            assertEquals("rate_limit_error", response.body!!.error.type)
            assertEquals("Rate limit exceeded", response.body!!.error.message)
            assertEquals("rate_limit", response.body!!.error.code)
        }

        @Test
        @DisplayName("NOT_FOUND 상태일 때 not_found_error 타입을 반환한다")
        fun `NOT_FOUND status returns not_found_error type`() {
            val ex = ApiException(
                status = HttpStatus.NOT_FOUND,
                message = "Not found",
                errorCode = "not_found",
            )

            val response = handler.handleApiException(ex)

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            assertEquals("not_found_error", response.body!!.error.type)
            assertEquals("Not found", response.body!!.error.message)
            assertEquals("not_found", response.body!!.error.code)
        }

        @Test
        @DisplayName("BAD_REQUEST 상태일 때 invalid_request_error 타입을 반환한다")
        fun `BAD_REQUEST status returns invalid_request_error type`() {
            val ex = ApiException(
                status = HttpStatus.BAD_REQUEST,
                message = "Bad request",
                errorCode = "bad_request",
            )

            val response = handler.handleApiException(ex)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            assertEquals("invalid_request_error", response.body!!.error.type)
            assertEquals("Bad request", response.body!!.error.message)
            assertEquals("bad_request", response.body!!.error.code)
        }

        @Test
        @DisplayName("INTERNAL_SERVER_ERROR 상태일 때 server_error 타입을 반환한다")
        fun `INTERNAL_SERVER_ERROR status returns server_error type`() {
            val ex = ApiException(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                message = "Server error",
                errorCode = "internal_error",
            )

            val response = handler.handleApiException(ex)

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
            assertEquals("server_error", response.body!!.error.type)
            assertEquals("Server error", response.body!!.error.message)
            assertEquals("internal_error", response.body!!.error.code)
        }

        @Test
        @DisplayName("errorCode가 null일 때 응답의 code 필드도 null이다")
        fun `null errorCode results in null code field`() {
            val ex = ApiException(
                status = HttpStatus.BAD_REQUEST,
                message = "Bad request",
            )

            val response = handler.handleApiException(ex)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            assertEquals("invalid_request_error", response.body!!.error.type)
            assertEquals(null, response.body!!.error.code)
        }
    }

    @Nested
    @DisplayName("handleInputException")
    inner class HandleInputException {

        @Test
        @DisplayName("ServerWebInputException 발생 시 400 상태와 invalid_request_error 타입을 반환한다")
        fun `returns 400 with invalid_request_error type`() {
            val ex = mockk<ServerWebInputException>()
            every { ex.message } returns "Invalid input"

            val response = handler.handleInputException(ex)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            assertEquals("invalid_request_error", response.body!!.error.type)
            assertEquals("Invalid input", response.body!!.error.message)
            assertEquals("invalid_request", response.body!!.error.code)
        }
    }

    @Nested
    @DisplayName("handleGenericException")
    inner class HandleGenericException {

        @Test
        @DisplayName("일반 Exception 발생 시 500 상태와 server_error 타입을 반환한다")
        fun `returns 500 with server_error type`() {
            val ex = RuntimeException("Something went wrong")

            val response = handler.handleGenericException(ex)

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
            assertEquals("server_error", response.body!!.error.type)
            assertEquals("Internal server error", response.body!!.error.message)
            assertEquals("internal_error", response.body!!.error.code)
        }

        @Test
        @DisplayName("일반 Exception 발생 시 원본 예외 메시지가 아닌 'Internal server error'를 반환한다")
        fun `does not expose original exception message`() {
            val ex = RuntimeException("Sensitive database connection error details")

            val response = handler.handleGenericException(ex)

            assertEquals("Internal server error", response.body!!.error.message)
        }
    }
}
