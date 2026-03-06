package com.gateway.infrastructure.llm.claude

import com.gateway.api.dto.request.ChatCompletionRequest
import com.gateway.api.dto.request.Message
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ClaudeRequestMapperTest {

    @Nested
    @DisplayName("system 메시지 처리")
    inner class SystemMessageHandling {

        @Test
        @DisplayName("system 메시지가 분리되어 system 필드로 들어간다")
        fun systemMessageShouldBeSeparatedIntoSystemField() {
            val request = ChatCompletionRequest(
                model = "claude-3-opus-20240229",
                messages = listOf(
                    Message(role = "system", content = "You are a helpful assistant."),
                    Message(role = "user", content = "Hello"),
                ),
            )

            val result = ClaudeRequestMapper.toClaudeRequest(request)

            assertEquals("You are a helpful assistant.", result.system)
        }

        @Test
        @DisplayName("system 메시지가 없을 때 system 필드는 null이다")
        fun systemFieldShouldBeNullWhenNoSystemMessage() {
            val request = ChatCompletionRequest(
                model = "claude-3-opus-20240229",
                messages = listOf(
                    Message(role = "user", content = "Hello"),
                ),
            )

            val result = ClaudeRequestMapper.toClaudeRequest(request)

            assertNull(result.system)
        }

        @Test
        @DisplayName("여러 system 메시지가 줄바꿈으로 합쳐진다")
        fun multipleSystemMessagesShouldBeJoinedWithNewline() {
            val request = ChatCompletionRequest(
                model = "claude-3-opus-20240229",
                messages = listOf(
                    Message(role = "system", content = "You are a helpful assistant."),
                    Message(role = "system", content = "Always respond in Korean."),
                    Message(role = "user", content = "Hello"),
                ),
            )

            val result = ClaudeRequestMapper.toClaudeRequest(request)

            assertEquals("You are a helpful assistant.\nAlways respond in Korean.", result.system)
        }

        @Test
        @DisplayName("role이 system인 메시지는 messages에서 제거된다")
        fun systemRoleMessagesShouldBeRemovedFromMessages() {
            val request = ChatCompletionRequest(
                model = "claude-3-opus-20240229",
                messages = listOf(
                    Message(role = "system", content = "You are a helpful assistant."),
                    Message(role = "user", content = "Hello"),
                    Message(role = "assistant", content = "Hi there!"),
                ),
            )

            val result = ClaudeRequestMapper.toClaudeRequest(request)

            assertEquals(2, result.messages.size)
            assertEquals("user", result.messages[0].role)
            assertEquals("Hello", result.messages[0].content)
            assertEquals("assistant", result.messages[1].role)
            assertEquals("Hi there!", result.messages[1].content)
        }
    }

    @Nested
    @DisplayName("maxTokens 기본값 처리")
    inner class MaxTokensDefault {

        @Test
        @DisplayName("maxTokens가 null일 때 기본값 4096이 사용된다")
        fun defaultMaxTokensShouldBe4096WhenNull() {
            val request = ChatCompletionRequest(
                model = "claude-3-opus-20240229",
                messages = listOf(Message(role = "user", content = "Hello")),
                maxTokens = null,
            )

            val result = ClaudeRequestMapper.toClaudeRequest(request)

            assertEquals(4096, result.maxTokens)
        }

        @Test
        @DisplayName("maxTokens가 지정되면 해당 값이 사용된다")
        fun specifiedMaxTokensShouldBeUsed() {
            val request = ChatCompletionRequest(
                model = "claude-3-opus-20240229",
                messages = listOf(Message(role = "user", content = "Hello")),
                maxTokens = 1024,
            )

            val result = ClaudeRequestMapper.toClaudeRequest(request)

            assertEquals(1024, result.maxTokens)
        }
    }

    @Nested
    @DisplayName("필드 매핑")
    inner class FieldMapping {

        @Test
        @DisplayName("모든 필드가 정상적으로 매핑된다")
        fun allFieldsShouldBeMappedCorrectly() {
            val request = ChatCompletionRequest(
                model = "claude-3-opus-20240229",
                messages = listOf(
                    Message(role = "user", content = "Hello"),
                ),
                temperature = 0.7,
                topP = 0.9,
                stop = listOf("END", "STOP"),
                stream = true,
                maxTokens = 2048,
            )

            val result = ClaudeRequestMapper.toClaudeRequest(request)

            assertEquals("claude-3-opus-20240229", result.model)
            assertEquals(0.7, result.temperature)
            assertEquals(0.9, result.topP)
            assertEquals(listOf("END", "STOP"), result.stopSequences)
            assertEquals(true, result.stream)
            assertEquals(2048, result.maxTokens)
        }

        @Test
        @DisplayName("model 필드가 정상 매핑된다")
        fun modelFieldShouldBeMapped() {
            val request = ChatCompletionRequest(
                model = "claude-3-sonnet-20240229",
                messages = listOf(Message(role = "user", content = "Hello")),
            )

            val result = ClaudeRequestMapper.toClaudeRequest(request)

            assertEquals("claude-3-sonnet-20240229", result.model)
        }

        @Test
        @DisplayName("temperature가 정상 매핑된다")
        fun temperatureFieldShouldBeMapped() {
            val request = ChatCompletionRequest(
                model = "claude-3-opus-20240229",
                messages = listOf(Message(role = "user", content = "Hello")),
                temperature = 0.5,
            )

            val result = ClaudeRequestMapper.toClaudeRequest(request)

            assertEquals(0.5, result.temperature)
        }

        @Test
        @DisplayName("stop이 stopSequences로 매핑된다")
        fun stopFieldShouldBeMappedToStopSequences() {
            val request = ChatCompletionRequest(
                model = "claude-3-opus-20240229",
                messages = listOf(Message(role = "user", content = "Hello")),
                stop = listOf("STOP"),
            )

            val result = ClaudeRequestMapper.toClaudeRequest(request)

            assertEquals(listOf("STOP"), result.stopSequences)
        }

        @Test
        @DisplayName("stream이 정상 매핑된다")
        fun streamFieldShouldBeMapped() {
            val request = ChatCompletionRequest(
                model = "claude-3-opus-20240229",
                messages = listOf(Message(role = "user", content = "Hello")),
                stream = true,
            )

            val result = ClaudeRequestMapper.toClaudeRequest(request)

            assertEquals(true, result.stream)
        }

        @Test
        @DisplayName("messages가 ClaudeMessage로 정상 매핑된다")
        fun messagesShouldBeMappedToClaudeMessages() {
            val request = ChatCompletionRequest(
                model = "claude-3-opus-20240229",
                messages = listOf(
                    Message(role = "user", content = "What is AI?"),
                    Message(role = "assistant", content = "AI stands for Artificial Intelligence."),
                    Message(role = "user", content = "Tell me more."),
                ),
            )

            val result = ClaudeRequestMapper.toClaudeRequest(request)

            assertEquals(3, result.messages.size)
            assertEquals(ClaudeMessage(role = "user", content = "What is AI?"), result.messages[0])
            assertEquals(ClaudeMessage(role = "assistant", content = "AI stands for Artificial Intelligence."), result.messages[1])
            assertEquals(ClaudeMessage(role = "user", content = "Tell me more."), result.messages[2])
        }
    }
}
