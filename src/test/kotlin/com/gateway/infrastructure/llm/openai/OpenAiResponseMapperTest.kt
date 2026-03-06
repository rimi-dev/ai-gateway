package com.gateway.infrastructure.llm.openai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OpenAiResponseMapperTest {

    @Nested
    @DisplayName("toChatCompletionResponse")
    inner class ToChatCompletionResponse {

        @Test
        @DisplayName("모든 필드가 정상 매핑된다")
        fun allFieldsShouldBeMappedCorrectly() {
            val response = OpenAiChatCompletionResponse(
                id = "chatcmpl-abc123",
                objectType = "chat.completion",
                created = 1709000000L,
                model = "gpt-4-0613",
                choices = listOf(
                    OpenAiChoice(
                        index = 0,
                        message = OpenAiResponseMessage(role = "assistant", content = "Hello!"),
                        finishReason = "stop",
                    ),
                ),
                usage = OpenAiUsage(promptTokens = 10, completionTokens = 20, totalTokens = 30),
                systemFingerprint = "fp_abc123",
            )

            val result = OpenAiResponseMapper.toChatCompletionResponse(response)

            assertEquals("chatcmpl-abc123", result.id)
            assertEquals("chat.completion", result.objectType)
            assertEquals(1709000000L, result.created)
            assertEquals("gpt-4-0613", result.model)
            assertEquals(1, result.choices.size)
            assertEquals(0, result.choices[0].index)
            assertEquals("assistant", result.choices[0].message.role)
            assertEquals("Hello!", result.choices[0].message.content)
            assertEquals("stop", result.choices[0].finishReason)
            assertEquals(10, result.usage.promptTokens)
            assertEquals(20, result.usage.completionTokens)
            assertEquals(30, result.usage.totalTokens)
            assertEquals("fp_abc123", result.systemFingerprint)
        }

        @Test
        @DisplayName("message.content가 null일 때 빈 문자열로 처리된다")
        fun shouldReturnEmptyStringWhenContentIsNull() {
            val response = OpenAiChatCompletionResponse(
                id = "chatcmpl-abc123",
                objectType = "chat.completion",
                created = 1709000000L,
                model = "gpt-4-0613",
                choices = listOf(
                    OpenAiChoice(
                        index = 0,
                        message = OpenAiResponseMessage(role = "assistant", content = null),
                        finishReason = "stop",
                    ),
                ),
                usage = OpenAiUsage(promptTokens = 10, completionTokens = 0, totalTokens = 10),
            )

            val result = OpenAiResponseMapper.toChatCompletionResponse(response)

            assertEquals("", result.choices[0].message.content)
        }

        @Test
        @DisplayName("여러 choices가 정상 매핑된다")
        fun multipleChoicesShouldBeMapped() {
            val response = OpenAiChatCompletionResponse(
                id = "chatcmpl-abc123",
                objectType = "chat.completion",
                created = 1709000000L,
                model = "gpt-4-0613",
                choices = listOf(
                    OpenAiChoice(
                        index = 0,
                        message = OpenAiResponseMessage(role = "assistant", content = "Response A"),
                        finishReason = "stop",
                    ),
                    OpenAiChoice(
                        index = 1,
                        message = OpenAiResponseMessage(role = "assistant", content = "Response B"),
                        finishReason = "stop",
                    ),
                    OpenAiChoice(
                        index = 2,
                        message = OpenAiResponseMessage(role = "assistant", content = "Response C"),
                        finishReason = "length",
                    ),
                ),
                usage = OpenAiUsage(promptTokens = 10, completionTokens = 60, totalTokens = 70),
            )

            val result = OpenAiResponseMapper.toChatCompletionResponse(response)

            assertEquals(3, result.choices.size)
            assertEquals(0, result.choices[0].index)
            assertEquals("Response A", result.choices[0].message.content)
            assertEquals("stop", result.choices[0].finishReason)
            assertEquals(1, result.choices[1].index)
            assertEquals("Response B", result.choices[1].message.content)
            assertEquals(2, result.choices[2].index)
            assertEquals("Response C", result.choices[2].message.content)
            assertEquals("length", result.choices[2].finishReason)
        }

        @Test
        @DisplayName("systemFingerprint가 정상 매핑된다")
        fun systemFingerprintShouldBeMapped() {
            val response = OpenAiChatCompletionResponse(
                id = "chatcmpl-abc123",
                objectType = "chat.completion",
                created = 1709000000L,
                model = "gpt-4-0613",
                choices = listOf(
                    OpenAiChoice(
                        index = 0,
                        message = OpenAiResponseMessage(role = "assistant", content = "Hello!"),
                        finishReason = "stop",
                    ),
                ),
                usage = OpenAiUsage(promptTokens = 10, completionTokens = 20, totalTokens = 30),
                systemFingerprint = "fp_44709d6fcb",
            )

            val result = OpenAiResponseMapper.toChatCompletionResponse(response)

            assertEquals("fp_44709d6fcb", result.systemFingerprint)
        }

        @Test
        @DisplayName("systemFingerprint가 null이면 null로 매핑된다")
        fun systemFingerprintShouldBeNullWhenNull() {
            val response = OpenAiChatCompletionResponse(
                id = "chatcmpl-abc123",
                objectType = "chat.completion",
                created = 1709000000L,
                model = "gpt-4-0613",
                choices = listOf(
                    OpenAiChoice(
                        index = 0,
                        message = OpenAiResponseMessage(role = "assistant", content = "Hello!"),
                        finishReason = "stop",
                    ),
                ),
                usage = OpenAiUsage(promptTokens = 10, completionTokens = 20, totalTokens = 30),
                systemFingerprint = null,
            )

            val result = OpenAiResponseMapper.toChatCompletionResponse(response)

            assertNull(result.systemFingerprint)
        }

        @Test
        @DisplayName("usage가 정상 매핑된다")
        fun usageShouldBeMappedCorrectly() {
            val response = OpenAiChatCompletionResponse(
                id = "chatcmpl-abc123",
                objectType = "chat.completion",
                created = 1709000000L,
                model = "gpt-4-0613",
                choices = listOf(
                    OpenAiChoice(
                        index = 0,
                        message = OpenAiResponseMessage(role = "assistant", content = "Hello!"),
                        finishReason = "stop",
                    ),
                ),
                usage = OpenAiUsage(promptTokens = 150, completionTokens = 250, totalTokens = 400),
            )

            val result = OpenAiResponseMapper.toChatCompletionResponse(response)

            assertEquals(150, result.usage.promptTokens)
            assertEquals(250, result.usage.completionTokens)
            assertEquals(400, result.usage.totalTokens)
        }
    }

    @Nested
    @DisplayName("toChunkResponse")
    inner class ToChunkResponse {

        @Test
        @DisplayName("stream chunk가 정상 매핑된다")
        fun streamChunkShouldBeMapped() {
            val response = OpenAiStreamChunkResponse(
                id = "chatcmpl-stream-123",
                objectType = "chat.completion.chunk",
                created = 1709000000L,
                model = "gpt-4-0613",
                choices = listOf(
                    OpenAiStreamChoice(
                        index = 0,
                        delta = OpenAiStreamDelta(content = "Hello"),
                        finishReason = null,
                    ),
                ),
            )

            val result = OpenAiResponseMapper.toChunkResponse(response)

            assertEquals("chatcmpl-stream-123", result.id)
            assertEquals("chat.completion.chunk", result.objectType)
            assertEquals(1709000000L, result.created)
            assertEquals("gpt-4-0613", result.model)
            assertEquals(1, result.choices.size)
            assertEquals(0, result.choices[0].index)
            assertEquals("Hello", result.choices[0].delta.content)
            assertNull(result.choices[0].finishReason)
        }

        @Test
        @DisplayName("stream chunk에서 role이 포함된 경우 정상 매핑된다")
        fun streamChunkWithRoleShouldBeMapped() {
            val response = OpenAiStreamChunkResponse(
                id = "chatcmpl-stream-123",
                objectType = "chat.completion.chunk",
                created = 1709000000L,
                model = "gpt-4-0613",
                choices = listOf(
                    OpenAiStreamChoice(
                        index = 0,
                        delta = OpenAiStreamDelta(role = "assistant", content = null),
                        finishReason = null,
                    ),
                ),
            )

            val result = OpenAiResponseMapper.toChunkResponse(response)

            assertEquals("assistant", result.choices[0].delta.role)
            assertNull(result.choices[0].delta.content)
        }

        @Test
        @DisplayName("stream chunk에서 finishReason이 포함된 경우 정상 매핑된다")
        fun streamChunkWithFinishReasonShouldBeMapped() {
            val response = OpenAiStreamChunkResponse(
                id = "chatcmpl-stream-123",
                objectType = "chat.completion.chunk",
                created = 1709000000L,
                model = "gpt-4-0613",
                choices = listOf(
                    OpenAiStreamChoice(
                        index = 0,
                        delta = OpenAiStreamDelta(),
                        finishReason = "stop",
                    ),
                ),
            )

            val result = OpenAiResponseMapper.toChunkResponse(response)

            assertEquals("stop", result.choices[0].finishReason)
        }

        @Test
        @DisplayName("usage가 non-null일 때 정상 매핑된다")
        fun usageShouldBeMappedWhenNonNull() {
            val response = OpenAiStreamChunkResponse(
                id = "chatcmpl-stream-123",
                objectType = "chat.completion.chunk",
                created = 1709000000L,
                model = "gpt-4-0613",
                choices = listOf(
                    OpenAiStreamChoice(
                        index = 0,
                        delta = OpenAiStreamDelta(),
                        finishReason = "stop",
                    ),
                ),
                usage = OpenAiUsage(promptTokens = 50, completionTokens = 100, totalTokens = 150),
            )

            val result = OpenAiResponseMapper.toChunkResponse(response)

            assertNotNull(result.usage)
            assertEquals(50, result.usage!!.promptTokens)
            assertEquals(100, result.usage.completionTokens)
            assertEquals(150, result.usage.totalTokens)
        }

        @Test
        @DisplayName("usage가 null일 때 null로 매핑된다")
        fun usageShouldBeNullWhenNull() {
            val response = OpenAiStreamChunkResponse(
                id = "chatcmpl-stream-123",
                objectType = "chat.completion.chunk",
                created = 1709000000L,
                model = "gpt-4-0613",
                choices = listOf(
                    OpenAiStreamChoice(
                        index = 0,
                        delta = OpenAiStreamDelta(content = "text"),
                        finishReason = null,
                    ),
                ),
                usage = null,
            )

            val result = OpenAiResponseMapper.toChunkResponse(response)

            assertNull(result.usage)
        }

        @Test
        @DisplayName("systemFingerprint가 chunk에서 정상 매핑된다")
        fun systemFingerprintShouldBeMappedInChunk() {
            val response = OpenAiStreamChunkResponse(
                id = "chatcmpl-stream-123",
                objectType = "chat.completion.chunk",
                created = 1709000000L,
                model = "gpt-4-0613",
                choices = listOf(
                    OpenAiStreamChoice(
                        index = 0,
                        delta = OpenAiStreamDelta(content = "Hi"),
                        finishReason = null,
                    ),
                ),
                systemFingerprint = "fp_stream_abc",
            )

            val result = OpenAiResponseMapper.toChunkResponse(response)

            assertEquals("fp_stream_abc", result.systemFingerprint)
        }

        @Test
        @DisplayName("여러 choices가 있는 chunk가 정상 매핑된다")
        fun multipleChoicesInChunkShouldBeMapped() {
            val response = OpenAiStreamChunkResponse(
                id = "chatcmpl-stream-123",
                objectType = "chat.completion.chunk",
                created = 1709000000L,
                model = "gpt-4-0613",
                choices = listOf(
                    OpenAiStreamChoice(
                        index = 0,
                        delta = OpenAiStreamDelta(content = "A"),
                        finishReason = null,
                    ),
                    OpenAiStreamChoice(
                        index = 1,
                        delta = OpenAiStreamDelta(content = "B"),
                        finishReason = null,
                    ),
                ),
            )

            val result = OpenAiResponseMapper.toChunkResponse(response)

            assertEquals(2, result.choices.size)
            assertEquals(0, result.choices[0].index)
            assertEquals("A", result.choices[0].delta.content)
            assertEquals(1, result.choices[1].index)
            assertEquals("B", result.choices[1].delta.content)
        }
    }
}
