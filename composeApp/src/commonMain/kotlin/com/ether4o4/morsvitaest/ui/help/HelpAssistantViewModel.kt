package com.ether4o4.morsvitaest.ui.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ether4o4.morsvitaest.data.DataRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs the tap-to-help assistant bubble. This is the app's built-in, ready-to-go
 * AI (the free hosted tier — no API key needed), given a setup-coach persona so it
 * can explain the app in plain language and walk a new user through adding their own
 * models and connecting MCP tool servers.
 *
 * It deliberately runs single-turn replies against the free instance rather than the
 * user's selected default service, so help is available the instant the app is
 * installed, before anything is configured.
 */
class HelpAssistantViewModel(
    private val repository: DataRepository,
) : ViewModel() {

    data class Message(val fromUser: Boolean, val text: String)

    data class State(
        val messages: ImmutableList<Message> = persistentListOf(GREETING),
        val isLoading: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var askJob: Job? = null

    fun ask(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty() || _state.value.isLoading) return

        askJob?.cancel()
        askJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    messages = (it.messages + Message(fromUser = true, text = trimmed)).toImmutableList(),
                    isLoading = true,
                    error = null,
                )
            }
            try {
                val instanceId = repository.getFreeMode().instanceId
                val prompt = buildString {
                    append(SYSTEM_PREAMBLE)
                    append("\n\nUser question: ")
                    append(trimmed)
                }
                val answer = repository.askSilentlyWithInstance(instanceId, prompt)
                _state.update {
                    it.copy(
                        messages = (it.messages + Message(fromUser = false, text = answer.ifBlank { FALLBACK })).toImmutableList(),
                        isLoading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Couldn't reach the assistant. Check your connection and try again.",
                    )
                }
            }
        }
    }

    fun reset() {
        askJob?.cancel()
        _state.value = State()
    }

    companion object {
        val GREETING = Message(
            fromUser = false,
            text = "Hey — I'm your built-in assistant, ready to go with no setup. " +
                "Ask me anything, or tap a suggestion below. I can walk you through adding your " +
                "own AI models and connecting MCP tool servers.",
        )

        private const val FALLBACK =
            "I didn't catch that — try rephrasing, or use the quick buttons above to jump straight to a setup screen."

        /**
         * Plain-language map of the app so the free model answers setup questions
         * accurately. Kept short on purpose — the small free model follows brief,
         * concrete guidance better than a long manual.
         */
        private const val SYSTEM_PREAMBLE =
            "You are the built-in setup assistant for MorsVitaEst, an AI agent app. " +
                "Answer briefly, warmly, and in plain language for a non-technical user. " +
                "Facts about the app you must use when relevant:\n" +
                "- It ships ready to use with a free built-in AI; no API key is needed to start.\n" +
                "- To add your own AI provider (OpenAI, Anthropic, Gemini, Groq, OpenRouter, etc.): " +
                "open Settings > Services, pick the provider, and paste its API key.\n" +
                "- To connect an MCP tool server: open Settings > Tools, find MCP servers, and add the " +
                "server's URL (and any header/token it needs).\n" +
                "- The Workspace has three tabs: Chat, Multi chat (compare two models side by side), and " +
                "Shell (a sandbox terminal). The gear icon in the Workspace opens Settings.\n" +
                "- The Home screen has a news feed and boxes for Services, MCP, Hugging Face, and Ollama; " +
                "each box's gear jumps straight to its settings.\n" +
                "If the user wants to do one of these, give the short step list and tell them they can also " +
                "tap the quick buttons in this help panel to jump there directly. Keep replies under 120 words."
    }
}
