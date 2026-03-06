package com.gateway.infrastructure.llm.openai

import com.gateway.api.dto.request.ChatCompletionRequest
import com.gateway.api.dto.request.Message
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OpenAiRequestMapperTest {

    @Nested
    @DisplayName("필드 1:1 매핑")
    inner class FieldMapping {

        @Test
        @DisplayName("모든 필드가 1:1로 정상 매핑된다")
        fun allFieldsShouldBeMappedOneToOne() {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(
                    Message(role = "system", content = "You are a helpful assistant."),
                    Message(role = "user", content = "Hello"),
                ),
                temperature = 0.7,
                maxTokens = 2048,
                topP = 0.9,
                stream = true,
                stop = listOf("END"),
                presencePenalty = 0.5,
                frequencyPenalty = 0.3,
                user = "user-123",
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertEquals("gpt-4", result.model)
            assertEquals(2, result.messages.size)
            assertEquals(0.7, result.temperature)
            assertEquals(2048, result.maxTokens)
            assertEquals(0.9, result.topP)
            assertEquals(true, result.stream)
            assertEquals(listOf("END"), result.stop)
            assertEquals(0.5, result.presencePenalty)
            assertEquals(0.3, result.frequencyPenalty)
            assertEquals("user-123", result.user)
        }

        @Test
        @DisplayName("model 필드가 정상 매핑된다")
        fun modelFieldShouldBeMapped() {
            val request = ChatCompletionRequest(
                model = "gpt-4-turbo",
                messages = listOf(Message(role = "user", content = "Hello")),
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertEquals("gpt-4-turbo", result.model)
        }

        @Test
        @DisplayName("messages가 OpenAiMessage로 정상 매핑된다")
        fun messagesShouldBeMappedToOpenAiMessages() {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(
                    Message(role = "system", content = "Be concise."),
                    Message(role = "user", content = "Explain AI."),
                    Message(role = "assistant", content = "AI is..."),
                ),
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertEquals(3, result.messages.size)
            assertEquals("system", result.messages[0].role)
            assertEquals("Be concise.", result.messages[0].content)
            assertEquals("user", result.messages[1].role)
            assertEquals("Explain AI.", result.messages[1].content)
            assertEquals("assistant", result.messages[2].role)
            assertEquals("AI is...", result.messages[2].content)
        }
    }

    @Nested
    @DisplayName("optional 필드 null 처리")
    inner class OptionalFieldsNullHandling {

        @Test
        @DisplayName("temperature가 null이면 null로 매핑된다")
        fun temperatureShouldBeNullWhenNull() {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(Message(role = "user", content = "Hello")),
                temperature = null,
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertNull(result.temperature)
        }

        @Test
        @DisplayName("maxTokens가 null이면 null로 매핑된다")
        fun maxTokensShouldBeNullWhenNull() {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(Message(role = "user", content = "Hello")),
                maxTokens = null,
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertNull(result.maxTokens)
        }

        @Test
        @DisplayName("topP가 null이면 null로 매핑된다")
        fun topPShouldBeNullWhenNull() {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(Message(role = "user", content = "Hello")),
                topP = null,
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertNull(result.topP)
        }

        @Test
        @DisplayName("stop이 null이면 null로 매핑된다")
        fun stopShouldBeNullWhenNull() {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(Message(role = "user", content = "Hello")),
                stop = null,
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertNull(result.stop)
        }

        @Test
        @DisplayName("presencePenalty가 null이면 null로 매핑된다")
        fun presencePenaltyShouldBeNullWhenNull() {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(Message(role = "user", content = "Hello")),
                presencePenalty = null,
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertNull(result.presencePenalty)
        }

        @Test
        @DisplayName("frequencyPenalty가 null이면 null로 매핑된다")
        fun frequencyPenaltyShouldBeNullWhenNull() {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(Message(role = "user", content = "Hello")),
                frequencyPenalty = null,
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertNull(result.frequencyPenalty)
        }

        @Test
        @DisplayName("user가 null이면 null로 매핑된다")
        fun userShouldBeNullWhenNull() {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(Message(role = "user", content = "Hello")),
                user = null,
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertNull(result.user)
        }

        @Test
        @DisplayName("모든 optional 필드가 null일 때 정상 동작한다")
        fun shouldWorkWhenAllOptionalFieldsAreNull() {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(Message(role = "user", content = "Hello")),
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertEquals("gpt-4", result.model)
            assertNull(result.temperature)
            assertNull(result.maxTokens)
            assertNull(result.topP)
            assertEquals(false, result.stream)
            assertNull(result.stop)
            assertNull(result.presencePenalty)
            assertNull(result.frequencyPenalty)
            assertNull(result.user)
        }
    }

    @Nested
    @DisplayName("name 필드 보존")
    inner class NameFieldPreservation {

        @Test
        @DisplayName("messages의 name 필드가 보존된다")
        fun nameFieldShouldBePreserved() {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(
                    Message(role = "system", content = "You are a helpful assistant.", name = "system_prompt"),
                    Message(role = "user", content = "Hello", name = "user_a"),
                ),
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertEquals("system_prompt", result.messages[0].name)
            assertEquals("user_a", result.messages[1].name)
        }

        @Test
        @DisplayName("name이 null인 메시지도 정상 매핑된다")
        fun nullNameShouldBeMapped() {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(
                    Message(role = "user", content = "Hello", name = null),
                ),
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertNull(result.messages[0].name)
        }

        @Test
        @DisplayName("name이 있는 메시지와 없는 메시지가 혼합되어도 정상 매핑된다")
        fun mixedNameFieldsShouldBeMapped() {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(
                    Message(role = "user", content = "Hello", name = "user_a"),
                    Message(role = "assistant", content = "Hi there!"),
                    Message(role = "user", content = "How are you?", name = "user_b"),
                ),
            )

            val result = OpenAiRequestMapper.toOpenAiRequest(request)

            assertEquals("user_a", result.messages[0].name)
            assertNull(result.messages[1].name)
            assertEquals("user_b", result.messages[2].name)
        }
    }
}
