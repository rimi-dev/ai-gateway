package com.gateway.infrastructure.llm.claude

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ClaudeResponseMapperTest {

    @Nested
    @DisplayName("toChatCompletionResponse")
    inner class ToChatCompletionResponse {

        @Test
        @DisplayName("id, model, objectType이 정상 매핑된다")
        fun shouldMapBasicFields() {
            val response = ClaudeMessagesResponse(
                id = "msg_01XFDUDYJgAACzvnptvVoYEL",
                type = "message",
                role = "assistant",
                content = listOf(ClaudeContent(type = "text", text = "Hello!")),
                model = "claude-3-opus-20240229",
                stopReason = "end_turn",
                usage = ClaudeUsage(inputTokens = 10, outputTokens = 20),
            )

            val result = ClaudeResponseMapper.toChatCompletionResponse(response)

            assertEquals("msg_01XFDUDYJgAACzvnptvVoYEL", result.id)
            assertEquals("claude-3-opus-20240229", result.model)
            assertEquals("chat.completion", result.objectType)
        }

        @Test
        @DisplayName("choices가 정상 매핑된다")
        fun shouldMapChoices() {
            val response = ClaudeMessagesResponse(
                id = "msg_123",
                type = "message",
                role = "assistant",
                content = listOf(ClaudeContent(type = "text", text = "Hello!")),
                model = "claude-3-opus-20240229",
                stopReason = "end_turn",
                usage = ClaudeUsage(inputTokens = 10, outputTokens = 20),
            )

            val result = ClaudeResponseMapper.toChatCompletionResponse(response)

            assertEquals(1, result.choices.size)
            assertEquals(0, result.choices[0].index)
            assertEquals("assistant", result.choices[0].message.role)
            assertEquals("Hello!", result.choices[0].message.content)
        }

        @Test
        @DisplayName("usage가 정상 매핑된다")
        fun shouldMapUsage() {
            val response = ClaudeMessagesResponse(
                id = "msg_123",
                type = "message",
                role = "assistant",
                content = listOf(ClaudeContent(type = "text", text = "Hello!")),
                model = "claude-3-opus-20240229",
                stopReason = "end_turn",
                usage = ClaudeUsage(inputTokens = 100, outputTokens = 50),
            )

            val result = ClaudeResponseMapper.toChatCompletionResponse(response)

            assertEquals(100, result.usage.promptTokens)
            assertEquals(50, result.usage.completionTokens)
            assertEquals(150, result.usage.totalTokens)
        }

        @Test
        @DisplayName("content에서 type=text인 첫 번째 항목의 text를 사용한다")
        fun shouldUseFirstTextContent() {
            val response = ClaudeMessagesResponse(
                id = "msg_123",
                type = "message",
                role = "assistant",
                content = listOf(
                    ClaudeContent(type = "tool_use", text = null),
                    ClaudeContent(type = "text", text = "First text"),
                    ClaudeContent(type = "text", text = "Second text"),
                ),
                model = "claude-3-opus-20240229",
                stopReason = "end_turn",
                usage = ClaudeUsage(inputTokens = 10, outputTokens = 20),
            )

            val result = ClaudeResponseMapper.toChatCompletionResponse(response)

            assertEquals("First text", result.choices[0].message.content)
        }

        @Test
        @DisplayName("content가 비어있을 때 빈 문자열을 반환한다")
        fun shouldReturnEmptyStringWhenContentIsEmpty() {
            val response = ClaudeMessagesResponse(
                id = "msg_123",
                type = "message",
                role = "assistant",
                content = emptyList(),
                model = "claude-3-opus-20240229",
                stopReason = "end_turn",
                usage = ClaudeUsage(inputTokens = 10, outputTokens = 0),
            )

            val result = ClaudeResponseMapper.toChatCompletionResponse(response)

            assertEquals("", result.choices[0].message.content)
        }
    }

    @Nested
    @DisplayName("stop_reason 매핑")
    inner class StopReasonMapping {

        @Test
        @DisplayName("end_turn은 stop으로 매핑된다")
        fun endTurnShouldMapToStop() {
            val response = createResponseWithStopReason("end_turn")

            val result = ClaudeResponseMapper.toChatCompletionResponse(response)

            assertEquals("stop", result.choices[0].finishReason)
        }

        @Test
        @DisplayName("max_tokens는 length로 매핑된다")
        fun maxTokensShouldMapToLength() {
            val response = createResponseWithStopReason("max_tokens")

            val result = ClaudeResponseMapper.toChatCompletionResponse(response)

            assertEquals("length", result.choices[0].finishReason)
        }

        @Test
        @DisplayName("stop_sequence는 stop으로 매핑된다")
        fun stopSequenceShouldMapToStop() {
            val response = createResponseWithStopReason("stop_sequence")

            val result = ClaudeResponseMapper.toChatCompletionResponse(response)

            assertEquals("stop", result.choices[0].finishReason)
        }

        @Test
        @DisplayName("null은 stop으로 매핑된다")
        fun nullShouldMapToStop() {
            val response = createResponseWithStopReason(null)

            val result = ClaudeResponseMapper.toChatCompletionResponse(response)

            assertEquals("stop", result.choices[0].finishReason)
        }

        private fun createResponseWithStopReason(stopReason: String?): ClaudeMessagesResponse {
            return ClaudeMessagesResponse(
                id = "msg_123",
                type = "message",
                role = "assistant",
                content = listOf(ClaudeContent(type = "text", text = "Hello!")),
                model = "claude-3-opus-20240229",
                stopReason = stopReason,
                usage = ClaudeUsage(inputTokens = 10, outputTokens = 20),
            )
        }
    }

    @Nested
    @DisplayName("toChunkResponse")
    inner class ToChunkResponse {

        @Test
        @DisplayName("content_block_delta 이벤트가 정상 매핑된다")
        fun shouldMapContentBlockDeltaEvent() {
            val event = ClaudeStreamEvent(
                type = "content_block_delta",
                delta = ClaudeStreamDelta(type = "text_delta", text = "Hello"),
            )

            val result = ClaudeResponseMapper.toChunkResponse(event, "claude-3-opus-20240229")

            assertNotNull(result)
            assertEquals("chat.completion.chunk", result!!.objectType)
            assertEquals("claude-3-opus-20240229", result.model)
            assertEquals(1, result.choices.size)
            assertEquals(0, result.choices[0].index)
            assertEquals("Hello", result.choices[0].delta.content)
            assertNull(result.choices[0].finishReason)
        }

        @Test
        @DisplayName("content_block_delta에서 delta.text가 null이면 null을 반환한다")
        fun shouldReturnNullWhenDeltaTextIsNullInContentBlockDelta() {
            val event = ClaudeStreamEvent(
                type = "content_block_delta",
                delta = ClaudeStreamDelta(type = "text_delta", text = null),
            )

            val result = ClaudeResponseMapper.toChunkResponse(event, "claude-3-opus-20240229")

            assertNull(result)
        }

        @Test
        @DisplayName("message_start 이벤트가 정상 매핑된다")
        fun shouldMapMessageStartEvent() {
            val event = ClaudeStreamEvent(
                type = "message_start",
                message = ClaudeStreamMessage(
                    id = "msg_stream_123",
                    type = "message",
                    role = "assistant",
                    model = "claude-3-opus-20240229",
                    usage = ClaudeUsage(inputTokens = 25, outputTokens = 0),
                ),
            )

            val result = ClaudeResponseMapper.toChunkResponse(event, "claude-3-sonnet-20240229")

            assertNotNull(result)
            assertEquals("msg_stream_123", result!!.id)
            assertEquals("claude-3-opus-20240229", result.model)
            assertEquals("chat.completion.chunk", result.objectType)
            assertEquals(1, result.choices.size)
            assertEquals("assistant", result.choices[0].delta.role)
            assertNull(result.choices[0].delta.content)
            assertNull(result.choices[0].finishReason)
        }

        @Test
        @DisplayName("message_start에서 message가 null이면 기본값이 사용된다")
        fun shouldUseFallbackWhenMessageIsNullInMessageStart() {
            val event = ClaudeStreamEvent(
                type = "message_start",
                message = null,
            )

            val result = ClaudeResponseMapper.toChunkResponse(event, "claude-3-opus-20240229")

            assertNotNull(result)
            assertEquals("chatcmpl-stream", result!!.id)
            assertEquals("claude-3-opus-20240229", result.model)
        }

        @Test
        @DisplayName("message_delta 이벤트에 usage가 포함되면 정상 매핑된다")
        fun shouldMapMessageDeltaEventWithUsage() {
            val event = ClaudeStreamEvent(
                type = "message_delta",
                delta = ClaudeStreamDelta(stopReason = "end_turn"),
                usage = ClaudeUsage(inputTokens = 25, outputTokens = 100),
            )

            val result = ClaudeResponseMapper.toChunkResponse(event, "claude-3-opus-20240229")

            assertNotNull(result)
            assertEquals("chat.completion.chunk", result!!.objectType)
            assertEquals("claude-3-opus-20240229", result.model)
            assertEquals("stop", result.choices[0].finishReason)
            assertNotNull(result.usage)
            assertEquals(25, result.usage!!.promptTokens)
            assertEquals(100, result.usage!!.completionTokens)
            assertEquals(125, result.usage!!.totalTokens)
        }

        @Test
        @DisplayName("message_delta 이벤트에 usage가 null이면 usage도 null이다")
        fun shouldReturnNullUsageWhenUsageIsNullInMessageDelta() {
            val event = ClaudeStreamEvent(
                type = "message_delta",
                delta = ClaudeStreamDelta(stopReason = "max_tokens"),
                usage = null,
            )

            val result = ClaudeResponseMapper.toChunkResponse(event, "claude-3-opus-20240229")

            assertNotNull(result)
            assertEquals("length", result!!.choices[0].finishReason)
            assertNull(result.usage)
        }

        @Test
        @DisplayName("알 수 없는 이벤트 타입(ping 등)에 대해 null을 반환한다")
        fun shouldReturnNullForUnknownEventTypes() {
            val pingEvent = ClaudeStreamEvent(type = "ping")

            val result = ClaudeResponseMapper.toChunkResponse(pingEvent, "claude-3-opus-20240229")

            assertNull(result)
        }

        @Test
        @DisplayName("content_block_start 등 처리하지 않는 이벤트에 대해 null을 반환한다")
        fun shouldReturnNullForUnhandledEventTypes() {
            val event = ClaudeStreamEvent(
                type = "content_block_start",
                index = 0,
                contentBlock = ClaudeContent(type = "text"),
            )

            val result = ClaudeResponseMapper.toChunkResponse(event, "claude-3-opus-20240229")

            assertNull(result)
        }
    }
}
