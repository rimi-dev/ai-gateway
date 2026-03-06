package com.gateway.domain.service

import com.gateway.api.exception.ApiException
import com.gateway.domain.model.ApiKey
import com.gateway.domain.model.Permission
import com.gateway.domain.repository.ApiKeyRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class ApiKeyServiceTest {

    private lateinit var apiKeyRepository: ApiKeyRepository
    private lateinit var apiKeyService: ApiKeyService

    @BeforeEach
    fun setUp() {
        apiKeyRepository = mockk()
        apiKeyService = ApiKeyService(apiKeyRepository)
    }

    @Nested
    @DisplayName("validateAndGet")
    inner class ValidateAndGet {

        @Test
        @DisplayName("유효한 키로 호출 시 ApiKey를 반환한다")
        fun `returns ApiKey when key is valid and enabled`() = runTest {
            // given
            val validKey = "gw-test-valid-key-12345"
            val expectedApiKey = ApiKey(
                id = "key-001",
                key = validKey,
                name = "Test API Key",
                teamId = "team-001",
                projectId = "project-001",
                permissions = setOf(Permission.CHAT),
                allowedModels = setOf("gpt-4o"),
                enabled = true,
            )
            coEvery { apiKeyRepository.findByKey(validKey) } returns expectedApiKey

            // when
            val result = apiKeyService.validateAndGet(validKey)

            // then
            assertNotNull(result)
            assertEquals(expectedApiKey.id, result.id)
            assertEquals(expectedApiKey.key, result.key)
            assertEquals(expectedApiKey.name, result.name)
            assertEquals(expectedApiKey.teamId, result.teamId)
            assertEquals(true, result.enabled)
        }

        @Test
        @DisplayName("존재하지 않는 키로 호출 시 UNAUTHORIZED ApiException을 던진다")
        fun `throws UNAUTHORIZED ApiException when key does not exist`() = runTest {
            // given
            val invalidKey = "gw-nonexistent-key"
            coEvery { apiKeyRepository.findByKey(invalidKey) } returns null

            // when & then
            val exception = assertThrows<ApiException> {
                apiKeyService.validateAndGet(invalidKey)
            }

            assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
            assertEquals("invalid_api_key", exception.errorCode)
            assertEquals("Invalid API key", exception.message)
        }

        @Test
        @DisplayName("비활성화된 키로 호출 시 FORBIDDEN ApiException을 던진다")
        fun `throws FORBIDDEN ApiException when key is disabled`() = runTest {
            // given
            val disabledKey = "gw-disabled-key-99999"
            val disabledApiKey = ApiKey(
                id = "key-002",
                key = disabledKey,
                name = "Disabled API Key",
                teamId = "team-002",
                enabled = false,
            )
            coEvery { apiKeyRepository.findByKey(disabledKey) } returns disabledApiKey

            // when & then
            val exception = assertThrows<ApiException> {
                apiKeyService.validateAndGet(disabledKey)
            }

            assertEquals(HttpStatus.FORBIDDEN, exception.status)
            assertEquals("api_key_disabled", exception.errorCode)
            assertEquals("API key is disabled", exception.message)
        }
    }
}
