package com.ether4o4.morsvitaest.network.dtos.gemini

import com.ether4o4.morsvitaest.data.TokenUsage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GeminiChatResponseDto(
    val candidates: List<Candidate>,
    val usageMetadata: UsageMetadata? = null,
) {
    @Serializable
    data class UsageMetadata(
        val promptTokenCount: Long? = null,
        val candidatesTokenCount: Long? = null,
        val thoughtsTokenCount: Long? = null,
        val totalTokenCount: Long? = null,
    )

    @Serializable
    data class Candidate(val content: Content? = null)

    @Serializable
    data class Content(val parts: List<Part>? = null)

    @Serializable
    data class Part(
        val text: String? = null,
        val functionCall: FunctionCall? = null,
        val thoughtSignature: String? = null,
        val thought: Boolean? = null,
    ) {
        val isThought: Boolean get() = thought == true
    }

    @Serializable
    data class FunctionCall(
        val name: String,
        val args: Map<String, JsonElement>? = null,
    )
}

fun GeminiChatResponseDto.extractText(): String = candidates.firstOrNull()?.content?.parts
    ?.filterNot { it.isThought }
    ?.joinToString("\n") { it.text ?: "" }
    ?: ""

/**
 * Token usage reported by Gemini, or null when absent. Thinking tokens are billed as output, so
 * they're folded into the output total alongside the candidate (answer) tokens.
 */
fun GeminiChatResponseDto.tokenUsage(): TokenUsage? {
    val u = usageMetadata ?: return null
    if (u.promptTokenCount == null && u.candidatesTokenCount == null &&
        u.thoughtsTokenCount == null && u.totalTokenCount == null
    ) {
        return null
    }
    val input = u.promptTokenCount ?: 0L
    val output = (u.candidatesTokenCount ?: 0L) + (u.thoughtsTokenCount ?: 0L)
    // Fall back to total − input when the breakdown is missing but a total is present.
    val resolvedOutput = if (output == 0L && u.totalTokenCount != null) {
        (u.totalTokenCount - input).coerceAtLeast(0L)
    } else {
        output
    }
    return TokenUsage(inputTokens = input.coerceAtLeast(0L), outputTokens = resolvedOutput)
}
