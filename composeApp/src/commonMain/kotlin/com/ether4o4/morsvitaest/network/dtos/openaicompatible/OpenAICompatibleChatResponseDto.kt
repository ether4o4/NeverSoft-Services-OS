package com.ether4o4.morsvitaest.network.dtos.openaicompatible

import com.ether4o4.morsvitaest.data.TokenUsage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val toolCallMarkerRegex = Regex("<TOOLCALL>[\\s\\S]*?</TOOLCALL>|<TOOLCALL>[\\s\\S]*$")

@Serializable
data class OpenAICompatibleChatResponseDto(
    val choices: List<Choice>,
    val usage: Usage? = null,
) {
    @Serializable
    data class Usage(
        @SerialName("prompt_tokens")
        val promptTokens: Long? = null,
        @SerialName("completion_tokens")
        val completionTokens: Long? = null,
        @SerialName("total_tokens")
        val totalTokens: Long? = null,
    )

    @Serializable
    data class Choice(val message: Message? = null) {
        @Serializable
        data class Message(
            val role: String? = null,
            val content: String? = null,
            // DeepSeek returns `reasoning_content`; OpenRouter returns `reasoning`.
            @SerialName("reasoning_content")
            val reasoningContent: String? = null,
            val reasoning: String? = null,
            @SerialName("tool_calls")
            val toolCalls: List<ToolCall>? = null,
        ) {
            /** Whichever reasoning field the provider used, normalized to one accessor. */
            val effectiveReasoning: String?
                get() = reasoningContent ?: reasoning

            /** Returns [content] if non-blank, otherwise falls back to reasoning. */
            val effectiveContent: String?
                get() {
                    val raw = content?.takeIf { it.isNotBlank() } ?: effectiveReasoning
                    // Some providers (e.g. Ollama) embed tool calls as <TOOLCALL>[...] markers
                    // in the content field alongside structured tool_calls — strip them.
                    if (raw != null && !toolCalls.isNullOrEmpty()) {
                        val stripped = raw.replace(toolCallMarkerRegex, "").trim()
                        return stripped.takeIf { it.isNotBlank() }
                    }
                    return raw
                }

            /** True when the effective content comes from reasoning rather than [content]. */
            val isContentFromReasoning: Boolean
                get() = content.isNullOrBlank() && !effectiveReasoning.isNullOrBlank()

            /**
             * Reasoning trace with the answer text trimmed off if the provider appended it.
             * LongCat (flash thinking) and a few others stream the final answer as the tail of
             * `reasoning_content`, then return the same text in `content` — without this, the
             * "Thinking" section duplicates the answer rendered below it.
             */
            fun reasoningTraceFor(answer: String?): String? {
                val reasoning = effectiveReasoning ?: return null
                if (answer.isNullOrBlank() || isContentFromReasoning) return reasoning
                val trimmedReasoning = reasoning.trimEnd()
                val trimmedAnswer = answer.trim()
                if (!trimmedReasoning.endsWith(trimmedAnswer)) return reasoning
                return trimmedReasoning.removeSuffix(trimmedAnswer).trimEnd().takeIf { it.isNotBlank() }
            }
        }
    }

    @Serializable
    data class ToolCall(
        val id: String,
        val type: String = "function",
        val function: FunctionCall,
    )

    @Serializable
    data class FunctionCall(
        val name: String,
        val arguments: String,
    )
}

/**
 * Token usage reported by the provider, or null when absent (some OpenAI-compatible endpoints
 * omit it). When only [Usage.totalTokens] is given, output is derived as total − prompt.
 */
fun OpenAICompatibleChatResponseDto.tokenUsage(): TokenUsage? {
    val u = usage ?: return null
    if (u.promptTokens == null && u.completionTokens == null && u.totalTokens == null) return null
    val input = u.promptTokens ?: 0L
    val output = u.completionTokens ?: u.totalTokens?.let { it - input } ?: 0L
    return TokenUsage(inputTokens = input.coerceAtLeast(0L), outputTokens = output.coerceAtLeast(0L))
}
