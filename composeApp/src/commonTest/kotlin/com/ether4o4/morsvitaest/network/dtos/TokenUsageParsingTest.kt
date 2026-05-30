package com.ether4o4.morsvitaest.network.dtos

import com.ether4o4.morsvitaest.network.dtos.anthropic.AnthropicChatResponseDto
import com.ether4o4.morsvitaest.network.dtos.gemini.GeminiChatResponseDto
import com.ether4o4.morsvitaest.network.dtos.openaicompatible.OpenAICompatibleChatResponseDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.ether4o4.morsvitaest.network.dtos.anthropic.tokenUsage as anthropicTokenUsage
import com.ether4o4.morsvitaest.network.dtos.gemini.tokenUsage as geminiTokenUsage
import com.ether4o4.morsvitaest.network.dtos.openaicompatible.tokenUsage as openAiTokenUsage

class TokenUsageParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun openai_usage_is_parsed() {
        val dto = json.decodeFromString<OpenAICompatibleChatResponseDto>(
            """{"choices":[{"message":{"content":"hi"}}],"usage":{"prompt_tokens":12,"completion_tokens":8,"total_tokens":20}}""",
        )
        val usage = dto.openAiTokenUsage()!!
        assertEquals(12L, usage.inputTokens)
        assertEquals(8L, usage.outputTokens)
        assertEquals(20L, usage.totalTokens)
    }

    @Test
    fun openai_without_usage_returns_null() {
        val dto = json.decodeFromString<OpenAICompatibleChatResponseDto>(
            """{"choices":[{"message":{"content":"hi"}}]}""",
        )
        assertNull(dto.openAiTokenUsage())
    }

    @Test
    fun openai_total_only_derives_output() {
        val dto = json.decodeFromString<OpenAICompatibleChatResponseDto>(
            """{"choices":[],"usage":{"prompt_tokens":30,"total_tokens":50}}""",
        )
        val usage = dto.openAiTokenUsage()!!
        assertEquals(30L, usage.inputTokens)
        assertEquals(20L, usage.outputTokens)
    }

    @Test
    fun anthropic_usage_is_parsed() {
        val dto = json.decodeFromString<AnthropicChatResponseDto>(
            """{"content":[{"type":"text","text":"hi"}],"usage":{"input_tokens":40,"output_tokens":15}}""",
        )
        val usage = dto.anthropicTokenUsage()!!
        assertEquals(40L, usage.inputTokens)
        assertEquals(15L, usage.outputTokens)
    }

    @Test
    fun gemini_usage_folds_thinking_tokens_into_output() {
        val dto = json.decodeFromString<GeminiChatResponseDto>(
            """{"candidates":[],"usageMetadata":{"promptTokenCount":100,"candidatesTokenCount":50,"thoughtsTokenCount":25,"totalTokenCount":175}}""",
        )
        val usage = dto.geminiTokenUsage()!!
        assertEquals(100L, usage.inputTokens)
        assertEquals(75L, usage.outputTokens)
    }

    @Test
    fun gemini_without_usage_returns_null() {
        val dto = json.decodeFromString<GeminiChatResponseDto>("""{"candidates":[]}""")
        assertNull(dto.geminiTokenUsage())
    }
}
