package com.ether4o4.morsvitaest.network.dtos.anthropic

import com.ether4o4.morsvitaest.data.TokenUsage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AnthropicChatResponseDto(
    val content: List<ContentBlock> = emptyList(),
    val stop_reason: String? = null,
    val usage: Usage? = null,
) {
    @Serializable
    data class ContentBlock(
        val type: String,
        val text: String? = null,
        val id: String? = null,
        val name: String? = null,
        val input: JsonObject? = null,
    )

    @Serializable
    data class Usage(
        @SerialName("input_tokens")
        val inputTokens: Long? = null,
        @SerialName("output_tokens")
        val outputTokens: Long? = null,
    )
}

fun AnthropicChatResponseDto.extractText(): String = content.filter { it.type == "text" }.mapNotNull { it.text }.joinToString("\n")

/** Token usage reported by Anthropic, or null when absent. */
fun AnthropicChatResponseDto.tokenUsage(): TokenUsage? {
    val u = usage ?: return null
    if (u.inputTokens == null && u.outputTokens == null) return null
    return TokenUsage(
        inputTokens = (u.inputTokens ?: 0L).coerceAtLeast(0L),
        outputTokens = (u.outputTokens ?: 0L).coerceAtLeast(0L),
    )
}
